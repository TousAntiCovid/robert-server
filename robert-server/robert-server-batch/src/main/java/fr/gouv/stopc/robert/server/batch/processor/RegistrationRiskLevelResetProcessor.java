package fr.gouv.stopc.robert.server.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RegistrationRiskLevelResetProcessor implements ItemProcessor<Registration, Registration> {

    public static final String USER_AT_RISK_NOT_NOTIFIED_MESSAGE = "Reseting risk level of a user never notified!";
    private final PropertyLoader propertyLoader;
    private int nbEpochsRiskLevelRetention;
    
    public RegistrationRiskLevelResetProcessor(PropertyLoader propertyLoader) {
        super();
        this.propertyLoader = propertyLoader;
        this.nbEpochsRiskLevelRetention = TimeUtils.EPOCHS_PER_DAY * this.propertyLoader.getRiskLevelRetentionPeriodInDays();
    }

    @Override
    public Registration process(Registration registration) throws Exception {
        if (registration.isAtRisk()) {
            int nbEpochsSinceLastScoringAtRisk = registration.getLastStatusRequestEpoch() - registration.getLatestRiskEpoch();
            if (nbEpochsSinceLastScoringAtRisk >= nbEpochsRiskLevelRetention) {
                if ( !registration.isNotified() ) {
                    log.info(USER_AT_RISK_NOT_NOTIFIED_MESSAGE);
                }
                registration.setAtRisk(false);
                // We do not reset registration#isNotified as it is used to compute the number of notifications in TAC 
                // It should evolve when a statistic table will be used to count notifications.
                return registration;
            }
        }
        return null;
    }

}
