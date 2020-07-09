package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.utils.ScoringUtils;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@AllArgsConstructor
public class PurgeOldEpochExpositionsProcessor implements ItemProcessor<Registration, Registration> {

    private IServerConfigurationService serverConfigurationService;

    @Override
    public Registration process(Registration registration) {
        log.debug("Purge Old Epoch Expositions started.");
        List<EpochExposition> exposedEpochs = registration.getExposedEpochs();

        // Exposed epochs should be empty, never null
        if (Objects.isNull(exposedEpochs)) {
            exposedEpochs = new ArrayList<>();
        }

        int currentEpochId = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        List<EpochExposition> epochsToKeep = ScoringUtils.getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
                exposedEpochs,
                currentEpochId,
                this.serverConfigurationService.getContagiousPeriod(),
                this.serverConfigurationService.getEpochDurationSecs());

        registration.setExposedEpochs(epochsToKeep);

        return registration;
    }
}
