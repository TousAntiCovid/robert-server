package fr.gouv.stopc.robertserver.database.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import fr.gouv.stopc.robertserver.database.repository.ItemIdMappingRepository;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;

@Service
public class ItemIdMappingServiceImpl<T> implements ItemIdMappingService {

    private ItemIdMappingRepository itemIdMappingRepository;

    @Inject
    public ItemIdMappingServiceImpl(ItemIdMappingRepository itemIdMappingRepository) {
        this.itemIdMappingRepository = itemIdMappingRepository;
    }


    @Override
    public void deleteAll() {
        itemIdMappingRepository.deleteAll();
    }

    @Override
    public List<T> getItemIdMappingsBetweenIds(long startId, long endId) {

        return itemIdMappingRepository.getItemIdMappingsBetweenIds(startId, endId).stream()
                .map(item -> (T)item.getItemId())
                .collect(Collectors.toList());
    }
}
