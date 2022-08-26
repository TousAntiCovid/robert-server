package fr.gouv.stopc.robert.server.batch.service;

import com.mongodb.MongoException;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.impl.RegistrationService;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static java.time.Instant.now;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEvaluationService {

    private final RegistrationService registrationService;

    private final BatchRegistrationService batchRegistrationService;

    private final MongoTemplate mongoTemplate;

    private final IServerConfigurationService serverConfigurationService;

    private final PropertyLoader propertyLoader;

    private final MetricsService metricsService;

    private Instant batchExecutionInstant;

    /**
     * Assuming the purge of the oldest epochs must be done before calling this
     * service
     **/
    @Timed(value = "robert.batch", extraTags = { "operation", "REGISTRATION_RISK_EVALUATION_STEP" })
    public void performs() {
        log.info("START : Risk Evaluation");

        // Value number of registrations that'll be processed
        long totalItemCount = registrationService.count();
        metricsService.setTotalRegistrationRiskEvaluationValued(totalItemCount);

        batchExecutionInstant = now();

        final var query = new Query().addCriteria(Criteria.where("outdatedRisk").is(true));

        mongoTemplate.executeQuery(
                query,
                "idTable",
                new RiskEvaluationService.RiskEvaluationRowCallbackHandler()
        );

        log.info("END : Risk Evaluation.");
    }

    private class RiskEvaluationRowCallbackHandler implements DocumentCallbackHandler {

        @Override
        @Counted(value = "REGISTRATION_RISK_EVALUATION_WORKER_STEP")
        public void processDocument(@NotNull Document document) throws MongoException, DataAccessException {
            final var registration = mongoTemplate.getConverter().read(Registration.class, document);

            batchRegistrationService.updateRegistrationIfRisk(
                    registration,
                    serverConfigurationService.getServiceTimeStart(),
                    propertyLoader.getRiskThreshold(),
                    batchExecutionInstant
            );

            registration.setOutdatedRisk(false);
            registrationService.saveRegistration(registration);
        }
    }
}
