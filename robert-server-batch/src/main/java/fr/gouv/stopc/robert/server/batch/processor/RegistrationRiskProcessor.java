package fr.gouv.stopc.robert.server.batch.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.utils.ScoringUtils;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRiskProcessor implements ItemProcessor<Registration, Registration> {

    private IServerConfigurationService serverConfigurationService;

    private ScoringStrategyService scoringStrategy;

    private PropertyLoader propertyLoader;


    @Override
    public Registration process(Registration registration) throws Exception {
        
        if (Objects.isNull(registration)) {
            return null;
        }
        
        List<EpochExposition> exposedEpochs = registration.getExposedEpochs();

        // Exposed epochs should be empty, never null
        if (Objects.isNull(exposedEpochs)) {
            exposedEpochs = new ArrayList<>();
        }

        int currentEpochId = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        List<EpochExposition> epochsToKeep = ScoringUtils.getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
                exposedEpochs,
                currentEpochId,
                this.propertyLoader.getContagiousPeriod(),
                this.serverConfigurationService.getEpochDurationSecs());
        registration.setExposedEpochs(epochsToKeep);

       ScoringUtils.updateRegistrationIfRisk(
                registration,
                epochsToKeep,
                this.serverConfigurationService.getServiceTimeStart(),
                this.propertyLoader.getRiskThreshold(),
                this.scoringStrategy
        );
       
        return registration;
    }

}
