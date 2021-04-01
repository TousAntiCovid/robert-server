package fr.gouv.clea.identification.reader;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collections;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.JdbcPagingItemReader;

import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.entity.ExposedVisit;
import lombok.Setter;

public class SinglePlaceExposedVisitItemReader implements ItemReader<SinglePlaceExposedVisits>, ItemStream {

    @Setter
    private JdbcPagingItemReader<ExposedVisit> delegate;

    @Override
    public SinglePlaceExposedVisits read() throws Exception {

        SinglePlaceExposedVisits singlePlaceExposedVisits = null;

        for (ExposedVisit visit; (visit = this.delegate.read()) != null; ) {
            if (ofNullable(singlePlaceExposedVisits).isEmpty()) {
                singlePlaceExposedVisits = createNewPlace(visit);
            } else {
                singlePlaceExposedVisits.addVisit(visit);
            }
        }
        return singlePlaceExposedVisits;
    }

    private SinglePlaceExposedVisits createNewPlace(final ExposedVisit exposedVisit) {
        return SinglePlaceExposedVisits.builder()
                .venueCategory1(exposedVisit.getVenueCategory1())
                .venueCategory2(exposedVisit.getVenueCategory2())
                .locationTemporaryPublicId(exposedVisit.getLocationTemporaryPublicId())
                .visits(new ArrayList<>(Collections.singletonList(exposedVisit)))
                .build();
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
