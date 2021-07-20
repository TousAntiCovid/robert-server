package fr.gouv.stopc.robert.server.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.server.batch.utils.ItemProcessingCounterUtils;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.model.Registration;

/**
 * Create ItemIdMapping objects from a Registration.
 */
public class RegistrationIdMappingProcessor implements ItemProcessor<Registration, ItemIdMapping<byte[]>> {

    @Override
    public ItemIdMapping process(Registration registration) {
        Long id = ItemProcessingCounterUtils.getInstance().incrementCurrentIdOfItemIdMapping();

        return ItemIdMapping.builder()
            .id(id)
            .itemId(registration.getPermanentIdentifier())
            .build();
    }
}
