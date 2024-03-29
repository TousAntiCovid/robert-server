package test.fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.repository.ItemIdMappingRepository;
import fr.gouv.stopc.robertserver.database.service.impl.ItemIdMappingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.verify;

@TestPropertySource("classpath:application.yml")
@ExtendWith(SpringExtension.class)
public class ItemIdMappingServiceTest {

    @InjectMocks
    private ItemIdMappingServiceImpl itemIdMappingService;

    @Mock
    ItemIdMappingRepository itemIdMappingRepository;

    @Test
    public void countNbUsersWithOldEpochExpositions() {

        // When
        this.itemIdMappingService.getItemIdMappingsBetweenIds(3, 10);

        // Then
        verify(this.itemIdMappingRepository).getItemIdMappingsBetweenIds(3, 10);
    }
}
