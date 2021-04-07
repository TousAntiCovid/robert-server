package fr.gouv.clea.indexation;

import fr.gouv.clea.prefixes.PrefixesStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.clea.config.BatchConstants.LTIDS_PARAM;

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

        final Map<String, List<String>> map = prefixesStorageService.getPrefixWithAssociatedLtidsMap();

        final Map<String, ExecutionContext> result = new HashMap<>();

        for (Map.Entry<String, List<String>> stringListEntry : map.entrySet()) {
            final ExecutionContext value = new ExecutionContext();
            value.put(LTIDS_PARAM, stringListEntry.getValue());
            value.put("prefix", stringListEntry.getKey());
            result.put("partition-" + stringListEntry.getKey(), value);
        }
        return result;
    }
}
