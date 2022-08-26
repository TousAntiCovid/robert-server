package fr.gouv.stopc.robert.server.batch.service;

import com.mongodb.MongoException;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
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

    @Timed(value = "robert.batch", extraTags = { "operation", "REGISTRATION_RISK_RESET_STEP" })
    public void performs() {
        log.info(
                "START : Reset risk level of registrations when retention time > {}.",
                propertyLoader.getRiskLevelRetentionPeriodInDays()
        );
        // Value number of registrations that'll be processed
        final long totalItemCount = registrationService.countNbUsersAtRisk();
        metricsService.setRobertBatchRiskLevelReset(totalItemCount);

        final var query = new Query().addCriteria(Criteria.where("atRisk").is(true));

        mongoTemplate.executeQuery(
                query,
                "idTable",
                new ReassessRiskRowCallbackHandler()
        );
        log.info("END : Reset risk level of registrations.");
    }

    private class ReassessRiskRowCallbackHandler implements DocumentCallbackHandler {

        @Override
        @Counted(value = "REGISTRATION_RISK_RESET_STEP_PROCEEDED_REGISTRATIONS")
        public void processDocument(@NotNull Document document) throws MongoException, DataAccessException {
            final var registration = mongoTemplate.getConverter().read(Registration.class, document);
            if (registration.isAtRisk() && riskRetentionThresholdHasExpired(registration)) {
                if (!registration.isNotified()) {
                    metricsService.incrementResettingRiskLevelOfNeverNotifiedUserCounter();
                } else {
                    metricsService.incrementResettingRiskLevelOfUsersAlreadyNotifiedCounter();
                }
                registration.setAtRisk(false);
                registration.setNotifiedForCurrentRisk(false);
                registrationService.saveRegistration(registration);
            }
        }

        private boolean riskRetentionThresholdHasExpired(final Registration registration) {
            final var today = robertClock.now().truncatedTo(DAYS);
            final var lastContactTime = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
            return lastContactTime.until(today).toDays() > propertyLoader.getRiskLevelRetentionPeriodInDays();
        }
    }
}
