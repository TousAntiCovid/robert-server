package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@RequiredArgsConstructor
public class RegistrationRiskLevelResetProcessor implements ItemProcessor<Registration, Registration> {

    private final PropertyLoader propertyLoader;

    private final RobertClock robertClock;

    @Override
    public Registration process(Registration registration) throws Exception {
        if (registration.isAtRisk() && riskRetentionThresholdHasExpired(registration)) {

            if (!registration.isNotified()) {
                log.info("Resetting risk level of a user never notified!");
            }
            registration.setAtRisk(false);
            // We do not reset registration#isNotified as it is used to compute the number
            // of notifications in TAC
            // It should evolve when a statistic table will be used to count notifications.
            registration.setNotifiedForCurrentRisk(false);
            return registration;
        }
        return null;
    }

    private boolean riskRetentionThresholdHasExpired(final Registration registration) {
        final var today = robertClock.now().truncatedTo(DAYS);
        final var lastContactTime = robertClock.atNtpTimestamp(registration.getLastContactTimestamp());
        return lastContactTime.until(today).toDays() > propertyLoader.getRiskLevelRetentionPeriodInDays();
    }

}
