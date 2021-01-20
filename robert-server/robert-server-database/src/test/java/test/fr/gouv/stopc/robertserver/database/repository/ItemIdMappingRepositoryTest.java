package test.fr.gouv.stopc.robertserver.database.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.stopc.robertserver.database.RobertServerDatabaseApplication;
import fr.gouv.stopc.robertserver.database.model.ItemIdMapping;
import fr.gouv.stopc.robertserver.database.repository.ItemIdMappingRepository;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RobertServerDatabaseApplication.class})
@DataMongoTest
public class ItemIdMappingRepositoryTest {

    @Autowired
    private ItemIdMappingRepository itemIdMappingRepository;

    @Test
    public void testGetItemIdMappingsBetweenIds() {
        // Given
        SecureRandom sr = new SecureRandom();

        byte[] rndBytes1 = new byte[5];
        sr.nextBytes(rndBytes1);

        byte[] rndBytes2 = new byte[5];
        sr.nextBytes(rndBytes2);

        byte[] rndBytes3 = new byte[5];
        sr.nextBytes(rndBytes3);

        byte[] rndBytes4 = new byte[5];
        sr.nextBytes(rndBytes4);

        byte[] rndBytes5 = new byte[5];
        sr.nextBytes(rndBytes5);

        ItemIdMapping rh1 = ItemIdMapping.builder().id(1l).itemId(rndBytes1).build();
        ItemIdMapping rh2 = ItemIdMapping.builder().id(2l).itemId(rndBytes2).build();
        ItemIdMapping rh3 = ItemIdMapping.builder().id(3l).itemId(rndBytes3).build();
        ItemIdMapping rh4 = ItemIdMapping.builder().id(4l).itemId(rndBytes4).build();
        ItemIdMapping rh5 = ItemIdMapping.builder().id(5l).itemId(rndBytes5).build();

        List<ItemIdMapping> regitrationIdMappings = new ArrayList<>();
        regitrationIdMappings.add(rh1);
        regitrationIdMappings.add(rh2);
        regitrationIdMappings.add(rh3);
        regitrationIdMappings.add(rh4);
        regitrationIdMappings.add(rh5);

        itemIdMappingRepository.saveAll(regitrationIdMappings);

        // When
        List<ItemIdMapping> itemIdMappings = itemIdMappingRepository.getItemIdMappingsBetweenIds(2, 4);

        // Then
        assertFalse(containsByteArray(itemIdMappings, rndBytes1));
        assertTrue(containsByteArray(itemIdMappings, rndBytes2));
        assertTrue(containsByteArray(itemIdMappings, rndBytes3));
        assertTrue(containsByteArray(itemIdMappings, rndBytes4));
        assertFalse(containsByteArray(itemIdMappings, rndBytes5));
    }

    private boolean containsByteArray(List<ItemIdMapping> itemIdMappings, byte[] byteArrayToFind){
        if(itemIdMappings
                .stream()
                .anyMatch(itemIdMapping -> Arrays.equals(((Binary)itemIdMapping.getItemId()).getData(), byteArrayToFind))) {
            return true;
        }
        return false;
    }

}
