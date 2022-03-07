package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.service.BatchRegistrationService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.AllArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.Objects;

/**
 * Evaluates the risk according to scores already computed.
 */
@AllArgsConstructor
public class RiskEvaluationProcessor implements ItemProcessor<Registration, Registration> {

    private IServerConfigurationService serverConfigurationService;

    private PropertyLoader propertyLoader;

    private BatchRegistrationService registrationService;

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
