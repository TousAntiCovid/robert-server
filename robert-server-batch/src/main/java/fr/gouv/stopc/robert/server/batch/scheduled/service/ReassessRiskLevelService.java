package fr.gouv.stopc.robert.server.batch.scheduled.service;

import com.mongodb.MongoException;
import fr.gouv.stopc.robert.server.batch.utils.MetricsService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.impl.RegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.DocumentCallbackHandler;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReassessRiskLevelService {

    private final RegistrationService registrationService;

    private final PropertyLoader propertyLoader;

    private final RobertClock robertClock;

    private final MongoTemplate mongoTemplate;

    private final MetricsService metricsService;

    public void process() {
        final var query = new Query();
        query.addCriteria(Criteria.where("atRisk").is(true));

        mongoTemplate.executeQuery(
                query,
                "idTable",
                new ReassessRiskRowCallbackHandler()
        );
    }

    private boolean riskRetentionThresholdHasExpired(final Registration registration) {
        final var today = robertClock.now().truncatedTo(DAYS);
        final var lastContactTime = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
        return lastContactTime.until(today).toDays() > propertyLoader.getRiskLevelRetentionPeriodInDays();
    }

    public Registration updateWhenAtRiskAndRetentionPeriodHasExpired(Registration registration) {
        if (registration.isAtRisk() && riskRetentionThresholdHasExpired(registration)) {
            if (!registration.isNotified()) {
                metricsService.addRessettingAlert();
            }
            registration.setAtRisk(false);
            registration.setNotifiedForCurrentRisk(false);
            return registration;
        }
        return null;
    }

    private class ReassessRiskRowCallbackHandler implements DocumentCallbackHandler {

        @Override
        public void processDocument(Document document) throws MongoException, DataAccessException {
            final var registration = mongoTemplate.getConverter().read(Registration.class, document);
            final var updatedRegistration = updateWhenAtRiskAndRetentionPeriodHasExpired(registration);
            if (null != updatedRegistration) {
                registrationService.saveRegistration(updatedRegistration);
            }
        }
    }
}
