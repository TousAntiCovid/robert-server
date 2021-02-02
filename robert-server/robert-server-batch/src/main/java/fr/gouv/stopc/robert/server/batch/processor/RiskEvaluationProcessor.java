package fr.gouv.stopc.robert.server.batch.processor;

import java.util.Objects;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class RiskEvaluationProcessor implements ItemProcessor<Registration, Registration> {

    private IServerConfigurationService serverConfigurationService;

    private ScoringStrategyService scoringStrategy;

    private PropertyLoader propertyLoader;

    private BatchRegistrationServiceImpl registrationService;

    @Override
    public Registration process(Registration registration) {
        if (Objects.isNull(registration)) {
            return null;
        }

        // Assuming the purge of the oldest epochs must be done before calling this processor
        // see fr.gouv.stopc.robert.server.batch.processor.PurgeOldEpochExpositionsProcessor

        boolean isRegistrationAtRisk = registrationService.updateRegistrationIfRisk(
                registration,
                this.serverConfigurationService.getServiceTimeStart(),
                this.propertyLoader.getRiskThreshold(),
                this.scoringStrategy
                );

        if (isRegistrationAtRisk){
            return registration;
        }

        return null;
    }
}
