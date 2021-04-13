package fr.gouv.clea.indexation;

import fr.gouv.clea.service.PrefixesStorageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import java.util.*;
import java.util.stream.Collectors;

import static fr.gouv.clea.config.BatchConstants.LTIDS_LIST_PARTITION_KEY;
import static fr.gouv.clea.config.BatchConstants.PREFIXES_PARTITION_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class IndexationPartitionerTest {

    @Mock
    private PrefixesStorageService prefixesStorageService;

    @InjectMocks
    private IndexationPartitioner indexationPartitioner;

    @Nested
    class GridSize_greater_than_mapSize {

        @Test
        void partition_returns_number_of_partitions_equal_to_mapSize() {

            Map<String, List<String>> prefixLtidsMap = getMap();
            Mockito.when(prefixesStorageService.getPrefixWithAssociatedLtidsMap()).thenReturn(prefixLtidsMap);

            final int gridSize = prefixLtidsMap.size() + 1;
            final Map<String, ExecutionContext> result = indexationPartitioner.partition(gridSize);

            assertThat(result).hasSameSizeAs(prefixLtidsMap);

            // verify presence of registered data to ensure nothing is missing
            verifyPrefixesCountMatchesInput(prefixLtidsMap, result);
            verifyLtidsListCountMatchesInput(prefixLtidsMap, result);
            verifyResultLtidsMatchInput(prefixLtidsMap, result);
        }
    }

    @Nested
    class MapSize_greater_than_gridSize {

        @Test
        void partition_returns_number_of_partitions_equal_to_gridSize() {

            Map<String, List<String>> prefixLtidsMap = getMap();
            Mockito.when(prefixesStorageService.getPrefixWithAssociatedLtidsMap()).thenReturn(prefixLtidsMap);

            final int gridSize = prefixLtidsMap.size() - 1;
            final Map<String, ExecutionContext> result = indexationPartitioner.partition(gridSize);

            assertThat(result).hasSize(gridSize);

            // retrieve all registered prefixes to ensure every prefix is present
            verifyPrefixesCountMatchesInput(prefixLtidsMap, result);
            verifyLtidsListCountMatchesInput(prefixLtidsMap, result);
            verifyResultLtidsMatchInput(prefixLtidsMap, result);
        }
    }

    @Nested
    class Multiple_items_per_partition {

        @Test
        void partition_returns_gridSize_number_of_partitions_with_multiple_results_per_partition() {
            // 10 prefixes
            Map<String, List<String>> prefixLtidsMap = getBigMap();
            Mockito.when(prefixesStorageService.getPrefixWithAssociatedLtidsMap()).thenReturn(prefixLtidsMap);

            final int gridSize = 4;
            final Map<String, ExecutionContext> result = indexationPartitioner.partition(gridSize);

            assertThat(result).hasSize(gridSize);

            // retrieve all registered prefixes to ensure every prefix is present
            verifyPrefixesCountMatchesInput(prefixLtidsMap, result);
            verifyLtidsListCountMatchesInput(prefixLtidsMap, result);
            verifyResultLtidsMatchInput(prefixLtidsMap, result);
            int itemsPerPartition = prefixLtidsMap.size() / gridSize;

            // 4*2 items + 1 on the 2 first partitions = 10 total
            assertThat((List<String>)(result.get("partition-0").get(PREFIXES_PARTITION_KEY))).hasSize(itemsPerPartition + 1);
            assertThat((List<String>)(result.get("partition-1").get(PREFIXES_PARTITION_KEY))).hasSize(itemsPerPartition + 1);
            assertThat((List<String>)(result.get("partition-2").get(PREFIXES_PARTITION_KEY))).hasSize(itemsPerPartition);
            assertThat((List<String>)(result.get("partition-3").get(PREFIXES_PARTITION_KEY))).hasSize(itemsPerPartition);
        }
    }

    private void verifyPrefixesCountMatchesInput(final Map<String, List<String>> inputMap, final Map<String, ExecutionContext> result) {
        List<String> registeredPrefixesList = result.values().stream()
                .map(executionContext -> (List<String>) executionContext.get(PREFIXES_PARTITION_KEY))
                .filter(Objects::nonNull)
                // stream of list of string to stream of string
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(registeredPrefixesList).hasSize(inputMap.size());
        assertThat(registeredPrefixesList).containsExactlyInAnyOrderElementsOf(new ArrayList<>(inputMap.keySet()));
    }

    private void verifyLtidsListCountMatchesInput(final Map<String, List<String>> inputMap, final Map<String, ExecutionContext> result) {
        List<List<String>> registeredPrefixesList = result.values().stream()
                .map(executionContext -> (List<List<String>>) executionContext.get(LTIDS_LIST_PARTITION_KEY))
                .filter(Objects::nonNull)
                // list of list of string to list of string
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(registeredPrefixesList).hasSize(inputMap.size());
    }

    private void verifyResultLtidsMatchInput(final Map<String, List<String>> map, final Map<String, ExecutionContext> result) {
        List<String> registeredPrefixesList = result.values().stream()
                .map(executionContext -> (List<List<String>>) executionContext.get(LTIDS_LIST_PARTITION_KEY))
                .filter(Objects::nonNull)
                // stream of list of list of string to stream of list of string
                .flatMap(Collection::stream)
                // stream of list of string to stream of string
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        final List<String> inputLtids = map.values().stream().flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(registeredPrefixesList).hasSameSizeAs(inputLtids);
        assertThat(registeredPrefixesList).containsExactlyInAnyOrderElementsOf(inputLtids);
    }

    private Map<String, List<String>> getMap() {
        final String prefix1 = "35";
        final List<String> ltidsList1 = List.of(
                "35ca2138-c742-4d18-ac3c-28b30c16272f",
                "35ca2138-c742-4d18-ac3c-28b30c16272h",
                "35ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix2 = "22";
        final List<String> ltidsList2 = List.of(
                "22ca2138-c742-4d18-ac3c-28b30c16272f",
                "22ca2138-c742-4d18-ac3c-28b30c16272h",
                "22ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix3 = "29";
        final List<String> ltidsList3 = List.of(
                "29ca2138-c742-4d18-ac3c-28b30c16272f",
                "29ca2138-c742-4d18-ac3c-28b30c16272h",
                "29ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix4 = "56";
        final List<String> ltidsList4 = List.of(
                "56ca2138-c742-4d18-ac3c-28b30c16272f",
                "56ca2138-c742-4d18-ac3c-28b30c16272h",
                "56ca2138-c742-4d18-ac3c-28b30c16272g");
        return Map.of(prefix1, ltidsList1, prefix2, ltidsList2, prefix3, ltidsList3, prefix4, ltidsList4);
    }

    private Map<String, List<String>> getBigMap() {
        final String prefix1 = "01";
        final List<String> ltidsList1 = List.of(
                "01ca2138-c742-4d18-ac3c-28b30c16272f",
                "01ca2138-c742-4d18-ac3c-28b30c16272h",
                "01ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix2 = "02";
        final List<String> ltidsList2 = List.of(
                "02ca2138-c742-4d18-ac3c-28b30c16272f",
                "02ca2138-c742-4d18-ac3c-28b30c16272h",
                "02ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix3 = "03";
        final List<String> ltidsList3 = List.of(
                "03ca2138-c742-4d18-ac3c-28b30c16272f",
                "03ca2138-c742-4d18-ac3c-28b30c16272h",
                "03ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix4 = "04";
        final List<String> ltidsList4 = List.of(
                "04ca2138-c742-4d18-ac3c-28b30c16272f",
                "04ca2138-c742-4d18-ac3c-28b30c16272h",
                "04ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix5 = "05";
        final List<String> ltidsList5 = List.of(
                "05ca2138-c742-4d18-ac3c-28b30c16272f",
                "05ca2138-c742-4d18-ac3c-28b30c16272h",
                "05ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix6 = "06";
        final List<String> ltidsList6 = List.of(
                "06ca2138-c742-4d18-ac3c-28b30c16272f",
                "06ca2138-c742-4d18-ac3c-28b30c16272h",
                "06ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix7 = "07";
        final List<String> ltidsList7 = List.of(
                "07ca2138-c742-4d18-ac3c-28b30c16272f",
                "07ca2138-c742-4d18-ac3c-28b30c16272h",
                "07ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix8 = "08";
        final List<String> ltidsList8 = List.of(
                "08ca2138-c742-4d18-ac3c-28b30c16272f",
                "08ca2138-c742-4d18-ac3c-28b30c16272h",
                "08ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix9 = "09";
        final List<String> ltidsList9 = List.of(
                "09ca2138-c742-4d18-ac3c-28b30c16272f",
                "09ca2138-c742-4d18-ac3c-28b30c16272h",
                "09ca2138-c742-4d18-ac3c-28b30c16272g");
        final String prefix10 = "10";
        final List<String> ltidsList10 = List.of(
                "10ca2138-c742-4d18-ac3c-28b30c16272f",
                "10ca2138-c742-4d18-ac3c-28b30c16272h",
                "10ca2138-c742-4d18-ac3c-28b30c16272g");
        return Map.of(prefix1, ltidsList1, prefix2, ltidsList2, prefix3, ltidsList3, prefix4, ltidsList4,
                prefix5, ltidsList5, prefix6, ltidsList6, prefix7, ltidsList7, prefix8, ltidsList8, prefix9, ltidsList9,
                prefix10, ltidsList10);
    }
}
