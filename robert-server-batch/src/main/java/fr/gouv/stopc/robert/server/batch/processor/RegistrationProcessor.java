package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.utils.ScoringUtils;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class RegistrationProcessor implements ItemProcessor<Registration, Registration> {

    private IServerConfigurationService serverConfigurationService;

    private IRegistrationService registrationService;

    private ScoringStrategyService scoringStrategy;

    private PropertyLoader propertyLoader;

    @Override
    public Registration process(Registration registration) {

        int currentEpochId = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        List<EpochExposition> epochsToKeep = ScoringUtils.getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
                registration.getExposedEpochs(),
                currentEpochId,
                this.serverConfigurationService.getContagiousPeriod(),
                this.serverConfigurationService.getEpochDurationSecs());

        boolean isRegistrationAtRisk = ScoringUtils.updateRegistrationIfRisk(
                registration,
                epochsToKeep,
                this.serverConfigurationService.getServiceTimeStart(),
                this.propertyLoader.getRiskThreshold(),
                this.scoringStrategy
                );

        if(isRegistrationAtRisk){
            return registration;
        }

        return null;
    }
}
