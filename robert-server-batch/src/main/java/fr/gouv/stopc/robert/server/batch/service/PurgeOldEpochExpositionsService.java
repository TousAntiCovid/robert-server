package fr.gouv.stopc.robert.server.batch.service;

import com.mongodb.MongoException;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.impl.RegistrationService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurgeOldEpochExpositionsService {

    private final MongoTemplate mongoTemplate;

    private final RegistrationService registrationService;

    private final BatchRegistrationService batchRegistrationService;

    private final IServerConfigurationService serverConfigurationService;

    private final PropertyLoader propertyLoader;

    private int computeMinOldEpochId() {
        final int currentEpochId = TimeUtils.getCurrentEpochFrom(serverConfigurationService.getServiceTimeStart());
        final int epochsByDay = 96;
        final int minEpochId = currentEpochId - propertyLoader.getContagiousPeriod() * epochsByDay;

        log.debug("Min EpochId for the purge of old epoch expositions : {}", minEpochId);

        return minEpochId;
    }

    @Timed(value = "robert.batch", extraTags = { "operation", "PURGE_OLD_EXPOSITIONS_STEP" })
    public void performs() {
        log.info("START : Purge Old Epoch Expositions.");

        final var query = new BasicQuery(
                "{exposedEpochs:{$elemMatch:{epochId:{$lte:" + computeMinOldEpochId() + "}}}}}"
        );

        mongoTemplate.executeQuery(
                query,
                "idTable",
                new PurgeOldEpochExpositionsRowCallbackHandler(serverConfigurationService, propertyLoader)
        );

        log.info("END : Purge Old Epoch Expositions.");
    }

    @RequiredArgsConstructor
    private class PurgeOldEpochExpositionsRowCallbackHandler implements DocumentCallbackHandler {

        private final IServerConfigurationService serverConfigurationService;

        private final PropertyLoader propertyLoader;

        @Override
        @Counted(value = "PURGE_OLD_EXPOSITIONS_STEP_PROCEEDED_REGISTRATIONS")
        public void processDocument(Document document) throws MongoException, DataAccessException {
            final var registration = mongoTemplate.getConverter().read(Registration.class, document);
            List<EpochExposition> exposedEpochs = new ArrayList<>(registration.getExposedEpochs());

            final int currentEpochId = TimeUtils
                    .getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());

            List<EpochExposition> epochsToKeep = batchRegistrationService
                    .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
                            exposedEpochs,
                            currentEpochId,
                            this.propertyLoader.getContagiousPeriod(),
                            this.serverConfigurationService.getEpochDurationSecs()
                    );

            if (!epochsToKeep.equals(exposedEpochs)) {
                registration.setExposedEpochs(epochsToKeep);
                registrationService.saveRegistration(registration);
            }
        }
    }
}
