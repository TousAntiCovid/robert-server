package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

@Slf4j
public class RegistrationRiskLevelResetProcessor implements ItemProcessor<Registration, Registration> {

    public static final String USER_AT_RISK_NOT_NOTIFIED_MESSAGE = "Reseting risk level of a user never notified!";

    private final PropertyLoader propertyLoader;

    private IServerConfigurationService serverConfigurationService;

    private int nbEpochsRiskLevelRetention;

    public RegistrationRiskLevelResetProcessor(PropertyLoader propertyLoader,
            IServerConfigurationService serverConfigurationService) {
        super();
        this.propertyLoader = propertyLoader;
        this.nbEpochsRiskLevelRetention = TimeUtils.EPOCHS_PER_DAY
                * this.propertyLoader.getRiskLevelRetentionPeriodInDays();
        this.serverConfigurationService = serverConfigurationService;
    }

    @Override
    public Registration process(Registration registration) throws Exception {
        if (registration.isAtRisk()) {

            LocalDateTime lastContactDateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(registration.getLastContactTimestamp()),
                    TimeZone.getDefault().toZoneId()
            );

            ZonedDateTime lastContactDateTimeWithTimeZone = lastContactDateTime
                    .atZone(TimeZone.getDefault().toZoneId());
            ZonedDateTime nowWithTimeZone = Instant.now().atZone(TimeZone.getDefault().toZoneId());

            if (Math.abs(
                    ChronoUnit.DAYS.between(lastContactDateTimeWithTimeZone, nowWithTimeZone)
            ) >= this.propertyLoader.getRiskLevelRetentionPeriodInDays()) {
                if (!registration.isNotified()) {
                    log.info(USER_AT_RISK_NOT_NOTIFIED_MESSAGE);
                }
                registration.setAtRisk(false);
                // We do not reset registration#isNotified as it is used to compute the number
                // of notifications in TAC
                // It should evolve when a statistic table will be used to count notifications.
                return registration;
            }
        }
        return null;
    }

}
