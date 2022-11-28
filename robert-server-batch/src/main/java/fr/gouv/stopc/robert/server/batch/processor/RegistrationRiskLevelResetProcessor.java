package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@RequiredArgsConstructor
public class RegistrationRiskLevelResetProcessor implements ItemProcessor<Registration, Registration> {

    private final PropertyLoader propertyLoader;

    private final RobertClock robertClock;

    @Override
    public Registration process(@NonNull final Registration registration) throws Exception {

        if (isAtRiskAndRetentionPeriodHasExpired(registration)) {
            if (!registration.isNotified()) {
                log.info("Resetting risk level of a user never notified!");
            }
            registration.setAtRisk(false);
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

    private boolean isAtRiskAndRetentionPeriodHasExpired(final Registration registration) {
        return registration.isAtRisk() && riskRetentionThresholdHasExpired(registration);
    }
}
