package fr.gouv.clea.indexation;

import fr.gouv.clea.service.PrefixesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.*;

@Slf4j
public class IndexationPartitioner implements Partitioner {

    private final PrefixesStorageService prefixesStorageService;

    public IndexationPartitioner(PrefixesStorageService prefixesStorageService) {
        this.prefixesStorageService = prefixesStorageService;
    }

    /**
     * Create a set of distinct {@link ExecutionContext} instances together with
     * a unique identifier for each one. The identifiers should be short,
     * mnemonic values, and only have to be unique within the return value (e.g.
     * use an incrementer).
     *
     * @param gridSize the size of the map to return
     * @return a map from identifier to input parameters
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        log.info("Computing indexation partitions...");
        final Map<String, List<String>> map = prefixesStorageService.getPrefixWithAssociatedLtidsMap();

        final Iterator<Map.Entry<String, List<String>>> mapIterator = map.entrySet().iterator();
        final Map<String, ExecutionContext> result = new HashMap<>();

        final int partitionSize = map.size()/gridSize;

        for (int partitionsIndex=0; partitionsIndex< gridSize; partitionsIndex++) {
            final ExecutionContext value = new ExecutionContext();
            final List<String> prefixes = new ArrayList<>();
            final List<List<String>> ltids = new ArrayList<>();
            for (int partitionIndex = 0; partitionIndex < partitionSize; partitionIndex++) {
                if (mapIterator.hasNext()) {
                    final var nextItem = mapIterator.next();
                    prefixes.add(nextItem.getKey());
                    ltids.add(nextItem.getValue());
                } else {
                    break;
                }
                value.put("prefixes", prefixes);
                value.put("ltids", ltids);
            }
            result.put("partition-"+partitionsIndex, value);
        }


//
//        for (Map.Entry<String, List<String>> stringListEntry : map.entrySet()) {
//            final ExecutionContext value = new ExecutionContext();
//            value.put(LTIDS_PARAM, stringListEntry.getValue());
//            value.put("prefix", stringListEntry.getKey());
//            result.put("partition-" + stringListEntry.getKey(), value);
//        }
        log.info("Partitions number: {}", result.size());
        return result;
    }
}
