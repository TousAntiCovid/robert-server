package fr.gouv.clea.indexation.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StepExecutionContextReaderTest {

    @Test
    void indexation_reader_returns_map_entries_corresponding_to_its_prefixes_and_ltidsLists_inputs() {
        final List<String> ltidsList1 = List.of("52f3fe14-2f58-4e26-b321-b73a51d411a1",
                "52f3fe14-2f58-4e26-b321-b73a51d411a2",
                "52f3fe14-2f58-4e26-b321-b73a51d411a3");
        final List<String> ltidsList2 = List.of("c7f878e0-25de-4bdc-9f62-a283b5133b3a",
                "c7f878e0-25de-4bdc-9f62-a283b5133b3b",
                "c7f878e0-25de-4bdc-9f62-a283b5133b3c");
        final List<List<String>> ltidsLists = List.of(ltidsList1, ltidsList2);

        final String prefix1 = "52";
        final String prefix2 = "c7";
        final List<String> prefixes = List.of(prefix1, prefix2);
        final StepExecutionContextReader reader = new StepExecutionContextReader(prefixes, ltidsLists);

        final Map<String, List<String>> resultMap = new HashMap<>();
        Map.Entry<String, List<String>> actualEntry;

        // simulate spring batch behaviour, looping on read() method until it returns null
        while ((actualEntry = reader.read()) != null) {
            resultMap.put(actualEntry.getKey(), actualEntry.getValue());
        }

        assertThat(resultMap).containsExactlyInAnyOrderEntriesOf(Map.of(prefix1, ltidsList1, prefix2, ltidsList2));
    }
}
