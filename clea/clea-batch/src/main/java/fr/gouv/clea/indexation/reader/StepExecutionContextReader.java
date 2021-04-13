package fr.gouv.clea.indexation.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StepExecutionContextReader implements ItemReader<Map.Entry<String, List<String>>> {

    private int index = 0;

    private final List<String> prefixes;

    private final List<List<String>> ltids;

    public StepExecutionContextReader(List<String> prefixes, List<List<String>> ltids) {
        this.prefixes = prefixes;
        this.ltids = ltids;
    }

    @Override
    public Map.Entry<String, List<String>> read() {
        if (!prefixes.isEmpty() && index < prefixes.size()) {
            AbstractMap.SimpleEntry<String, List<String>> singleMap = new AbstractMap.SimpleEntry<>(prefixes.get(index), ltids.get(index));
            index++;
            return singleMap;
        } else {
            return null;
        }
    }
}