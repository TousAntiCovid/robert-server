package fr.gouv.clea.prefixes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.JdbcCursorItemReader;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ListItemReader implements ItemReader<List<String>>, ItemStream {

    private final JdbcCursorItemReader<String> delegate;

    @Override
    public List<String> read() throws Exception {

        final List<String> ltidList = new ArrayList<>();

        for (String clusterLtid; (clusterLtid = this.delegate.read()) != null; ) {
            ltidList.add(clusterLtid);
        }
        //null return means all input data has been read, and forwards execution to processor
        return !ltidList.isEmpty() ? ltidList : null;
    }

    /**
     * Open the stream for the provided {@link ExecutionContext}.
     *
     * @param executionContext current step's {@link ExecutionContext}.  Will be the
     *                         executionContext from the last run of the step on a restart.
     * @throws IllegalArgumentException if context is null
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.delegate.open(executionContext);
    }

    /**
     * Indicates that the execution context provided during open is about to be saved. If any state is remaining, but
     * has not been put in the context, it should be added here.
     *
     * @param executionContext to be updated
     * @throws IllegalArgumentException if executionContext is null.
     */
    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        this.delegate.update(executionContext);
    }

    /**
     * If any resources are needed for the stream to operate they need to be destroyed here. Once this method has been
     * called all other methods (except open) may throw an exception.
     */
    @Override
    public void close() throws ItemStreamException {
        this.delegate.close();
    }
}
