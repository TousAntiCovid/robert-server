package fr.gouv.clea.indexation;

import fr.gouv.clea.service.PrefixesStorageService;
import org.assertj.core.api.Assertions;
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
            verifyLtidsCountMatchesInput(prefixLtidsMap, result);
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
            verifyLtidsCountMatchesInput(prefixLtidsMap, result);
        }
    }

    private void verifyPrefixesCountMatchesInput(final Map<String, List<String>> inputMap, final Map<String, ExecutionContext> result) {
        List<String> registeredPrefixesList = result.values().stream()
                .map(executionContext -> (List<String>) executionContext.get(PREFIXES_PARTITION_KEY))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(registeredPrefixesList).hasSize(inputMap.size());
        assertThat(registeredPrefixesList).containsExactlyInAnyOrderElementsOf(new ArrayList<>(inputMap.keySet()));
    }

    private void verifyLtidsListCountMatchesInput(final Map<String, List<String>> inputMap, final Map<String, ExecutionContext> result) {
        List<List<String>> registeredPrefixesList = result.values().stream()
                .map(executionContext -> (List<List<String>>) executionContext.get(LTIDS_LIST_PARTITION_KEY))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertThat(registeredPrefixesList).hasSize(inputMap.size());
    }

    private void verifyLtidsCountMatchesInput(final Map<String, List<String>> map, final Map<String, ExecutionContext> result) {
        List<String> registeredPrefixesList = result.values().stream()
                .map(executionContext -> (List<List<String>>) executionContext.get(LTIDS_LIST_PARTITION_KEY))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
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
}
