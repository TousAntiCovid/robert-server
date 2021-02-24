package fr.gouv.stopc.robert.server.batch.processor;

import org.springframework.batch.item.ItemProcessor;

import fr.gouv.stopc.robert.server.batch.utils.ItemProcessingCounterUtils;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Create ItemIdMapping objects from a Contact.
 */
public class ContactIdMappingProcessor implements ItemProcessor<Contact, ItemIdMapping<String>> {

    @Override
    public ItemIdMapping process(Contact contact) {
        Long id = ItemProcessingCounterUtils.getInstance().incrementCurrentIdOfItemIdMapping();

        return ItemIdMapping.builder()
                .id(id)
                .itemId(contact.getId())
                .build();
    }
}
