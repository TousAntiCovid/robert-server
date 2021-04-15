package fr.gouv.clea.indexation.reader;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StepExecutionContextReaderTest {

    @Test
    void indexation_reader_returns_map_entries_corresponding_to_its_prefixes_and_ltidsLists_inputs() {
        final List<String> ltidsList2 = List.of("c7f878e0-25de-4bdc-9f62-a283b5133b3a",
                "c7f878e0-25de-4bdc-9f62-a283b5133b3b",
                "c7f878e0-25de-4bdc-9f62-a283b5133b3c");
        final List<String> ltidsList1 = List.of("52f3fe14-2f58-4e26-b321-b73a51d411a1",
                "52f3fe14-2f58-4e26-b321-b73a51d411a2",
                "52f3fe14-2f58-4e26-b321-b73a51d411a3");
        final List<List<String>> ltidsLists = List.of(ltidsList1, ltidsList2);

        final String prefix1 = "52";
        final String prefix2 = "c7";
        final List<String> prefixes = List.of(prefix1, prefix2);
        StepExecutionContextReader reader = new StepExecutionContextReader(prefixes, ltidsLists);

        int index = 0;
        assertThat(reader.getIndex()).isEqualTo(index);
        final Map<Integer, Map.Entry<String, List<String>>> resultMap = new HashMap<>();
        Map.Entry<String, List<String>> entry;

        // simulate spring batch behaviour, looping on read() method until it returns null
        while ((entry = reader.read()) != null) {
            resultMap.put(index, entry);
            index++;
            assertThat(reader.getIndex()).isEqualTo(index);
        }

        for (int resultMapIndex=0; resultMapIndex< resultMap.size(); resultMapIndex++) {
            assertThat(resultMap.get(resultMapIndex).getKey()).isEqualTo(prefixes.get(resultMapIndex));
            assertThat(resultMap.get(resultMapIndex).getValue()).isEqualTo(ltidsLists.get(resultMapIndex));
        }
    }
}
