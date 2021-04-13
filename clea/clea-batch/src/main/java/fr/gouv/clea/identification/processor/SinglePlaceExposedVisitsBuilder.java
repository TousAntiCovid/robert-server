package fr.gouv.clea.identification.processor;

import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.entity.ExposedVisit;
import fr.gouv.clea.identification.ExposedVisitRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static fr.gouv.clea.config.BatchConstants.*;

/**
 * This class is executing in many Threads
 */
@Slf4j
@StepScope
public class SinglePlaceExposedVisitsBuilder implements ItemProcessor<String, SinglePlaceExposedVisits> {

    private final JdbcTemplate jdbcTemplate;

    private final AtomicLong counter = new AtomicLong();

    private final ExposedVisitRowMapper rowMapper;

    public SinglePlaceExposedVisitsBuilder(JdbcTemplate jdbcTemplate, ExposedVisitRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    @Override
    public SinglePlaceExposedVisits process(final String ltid) {
        final List<ExposedVisit> list = jdbcTemplate.query("select * from " + EXPOSED_VISITS_TABLE
                        + " WHERE ltid= ? ORDER BY " + PERIOD_COLUMN + ", " + TIMESLOT_COLUMN,
                rowMapper, UUID.fromString(ltid));
        ExposedVisit firstExposedVisit = list.stream().findFirst().orElse(null);
        if (null != firstExposedVisit) {
            long loadedVisitsCount = counter.incrementAndGet();
            if (0 == loadedVisitsCount % 1000) {
                log.info("Loaded {} visits, current LTId={} ", loadedVisitsCount, ltid);
            }
            return SinglePlaceExposedVisits.builder()
                    .locationTemporaryPublicId(firstExposedVisit.getLocationTemporaryPublicId())
                    .venueType(firstExposedVisit.getVenueType()).venueCategory1(firstExposedVisit.getVenueCategory1())
                    .venueCategory2(firstExposedVisit.getVenueCategory2()).visits(list).build();
        }
        return null;
    }

    @AfterStep
    public ExitStatus afterStep(ExecutionContext ctx) {
        log.info("building {} SinglePlaceExposedVisits", counter.get());
        return ExitStatus.COMPLETED;
    }
}
