package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.service.BatchRegistrationService;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.AllArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Purge scorings having too old epochs, i.e. more than
 * {propertyLoader.getContagiousPeriod()} days old.
 */
@AllArgsConstructor
public class PurgeOldEpochExpositionsProcessor implements ItemProcessor<Registration, Registration> {

    private IServerConfigurationService serverConfigurationService;

    private PropertyLoader propertyLoader;

    private BatchRegistrationService batchRegistrationService;

    @Override
    public Registration process(Registration registration) {
        List<EpochExposition> exposedEpochs = registration.getExposedEpochs();

        // Exposed epochs should be empty, never null
        if (Objects.isNull(exposedEpochs)) {
            exposedEpochs = new ArrayList<>();
        }

        int currentEpochId = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());
        List<EpochExposition> epochsToKeep = batchRegistrationService
                .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
                        exposedEpochs,
                        currentEpochId,
                        this.propertyLoader.getContagiousPeriod(),
                        this.serverConfigurationService.getEpochDurationSecs()
                );

        registration.setExposedEpochs(epochsToKeep);

        return registration;
    }
}
