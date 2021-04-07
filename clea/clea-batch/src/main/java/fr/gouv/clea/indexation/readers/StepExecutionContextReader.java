package fr.gouv.clea.indexation.readers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class StepExecutionContextReader implements ItemReader<Map.Entry<String, List<String>>> {

    private boolean isAlreadyDone = false;

    private final String prefix;

    private final List<String> ltids;

    public StepExecutionContextReader(String prefix, List<String> ltids) {
        this.prefix = prefix;
        this.ltids = ltids;
    }

    @Override
    public Map.Entry<String, List<String>> read() {
        if (isAlreadyDone) {
            return null;
        } else {
            isAlreadyDone = true;
            return new AbstractMap.SimpleEntry<>(prefix, ltids);
        }
    }
}

//public class MemoryMapItemReader extends IteratorItemReader<Map.Entry<String, List<String>>> {
//    public MemoryMapItemReader(Callable<Iterator<Map.Entry<String, List<String>>>> iterator) {
//        super(iterator);
//    }
//}