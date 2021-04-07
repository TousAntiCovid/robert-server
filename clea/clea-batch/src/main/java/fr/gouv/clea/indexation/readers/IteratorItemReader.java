package fr.gouv.clea.indexation.readers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class IteratorItemReader<T> implements ItemReader<T> {

    private Iterator<T> iterator = null;

    private final Callable<Iterator<T>> iteratorProvider;

    public IteratorItemReader(Callable<Iterator<T>> iteratorProvider) {
        this.iteratorProvider = iteratorProvider;
    }

    @Override
    public T read() throws Exception {

        if (iterator == null) {
            iterator = iteratorProvider.call();
        }
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}
