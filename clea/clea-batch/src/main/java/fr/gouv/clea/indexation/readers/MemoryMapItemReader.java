package fr.gouv.clea.indexation.readers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class MemoryMapItemReader extends IteratorItemReader<Map.Entry<String, List<String>>> {
    public MemoryMapItemReader(Callable<Iterator<Map.Entry<String, List<String>>>> iterator) {
        super(iterator);
    }
}
