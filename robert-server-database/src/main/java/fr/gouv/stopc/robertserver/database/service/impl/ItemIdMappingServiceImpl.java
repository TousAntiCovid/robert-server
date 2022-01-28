package fr.gouv.stopc.robertserver.database.service.impl;

import fr.gouv.stopc.robertserver.database.repository.ItemIdMappingRepository;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemIdMappingServiceImpl<T> implements ItemIdMappingService<T> {

    private final ItemIdMappingRepository itemIdMappingRepository;

    @Override
    public void deleteAll() {
        itemIdMappingRepository.deleteAll();
    }

    @Override
    public List<T> getItemIdMappingsBetweenIds(long startId, long endId) {

        return itemIdMappingRepository.getItemIdMappingsBetweenIds(startId, endId).stream()
                .map(item -> (T) item.getItemId())
                .collect(Collectors.toList());
    }
}
