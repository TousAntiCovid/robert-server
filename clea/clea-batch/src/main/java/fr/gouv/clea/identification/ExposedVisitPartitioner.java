package fr.gouv.clea.identification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.clea.config.BatchConstants.*;

@Slf4j
public class ExposedVisitPartitioner implements Partitioner {

    private final JdbcOperations jdbcTemplate;

    private int partitionsNumber = 0;

    public ExposedVisitPartitioner(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        final String selectDistinctLtidRequest = "SELECT DISTINCT " + LTID_COL + " FROM " + EXPOSED_VISITS_TABLE;
        final List<String> visitedPlaces = jdbcTemplate.queryForList(selectDistinctLtidRequest, String.class);

        Map<String, ExecutionContext> result = new HashMap<>();
        if (visitedPlaces.isEmpty()) {
        	return result;
        }
        
        String lastEvaluatedLtid = visitedPlaces.get(0);

        for (String currentLtid : visitedPlaces) {
            /*
             first item: create exec context, register first ltid in context, register context
             and create context for potential next ltid
             */
            if (result.isEmpty()) {
                lastEvaluatedLtid = createNewPartitionFromLtid(result, currentLtid);
            }
            /*
            different currentLtid value = new ltid: register it in exec context, save exec context and create new one for
            potential next ltid
             */
            if (!currentLtid.equals(lastEvaluatedLtid)) {
                lastEvaluatedLtid = createNewPartitionFromLtid(result, currentLtid);
            }
        }
        log.debug("Detected ltids: {}", result);
        return result;
    }

    private String createNewPartitionFromLtid(final Map<String, ExecutionContext> result, final String currentLtid) {
        ExecutionContext ctx = new ExecutionContext();
        ctx.put(LTID_PARAM, currentLtid);
        createPartitionFromContext(result, ctx);
        return currentLtid;
    }

    private void createPartitionFromContext(final Map<String, ExecutionContext> result, final ExecutionContext tmpExecutionContext) {
        result.put("partition" + partitionsNumber, tmpExecutionContext);
        partitionsNumber = result.size();
    }
}
