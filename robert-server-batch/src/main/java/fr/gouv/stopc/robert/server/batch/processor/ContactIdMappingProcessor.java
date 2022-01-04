package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.utils.ItemProcessingCounterUtils;
import fr.gouv.stopc.robert.server.batch.utils.MetricsService;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

/**
 * Create ItemIdMapping objects from a Contact.
 */
@RequiredArgsConstructor
public class ContactIdMappingProcessor implements ItemProcessor<Contact, ItemIdMapping<String>> {

    private final MetricsService metricsService;

    @Override
    public ItemIdMapping process(Contact contact) {
        Long id = ItemProcessingCounterUtils.getInstance().incrementCurrentIdOfItemIdMapping();

        if (contact.getMessageDetails() != null && !contact.getMessageDetails().isEmpty()) {
            metricsService.getRobertBatchHelloMessageTotal().increment(contact.getMessageDetails().size());
        }

        return ItemIdMapping.builder()
                .id(id)
                .itemId(contact.getId())
                .build();
    }
}
