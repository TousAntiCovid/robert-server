package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.service.BatchRegistrationService;
import fr.gouv.stopc.robert.server.batch.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.Objects;

/**
 * Evaluates the risk according to scores already computed.
 */
@RequiredArgsConstructor
public class RiskEvaluationProcessor implements ItemProcessor<Registration, Registration> {

    private final IServerConfigurationService serverConfigurationService;

    private final PropertyLoader propertyLoader;

    private final BatchRegistrationService registrationService;

    @Override
    public Registration process(Registration registration) {
        if (Objects.isNull(registration)) {
            return null;
        }

        // Assuming the purge of the oldest epochs must be done before calling this
        // processor
        // see
        // fr.gouv.stopc.robert.server.batch.processor.PurgeOldEpochExpositionsProcessor

        registrationService.updateRegistrationIfRisk(
                registration,
                this.serverConfigurationService.getServiceTimeStart(),
                this.propertyLoader.getRiskThreshold()
        );

        registration.setOutdatedRisk(false);
        return registration;

    }
}
