package fr.gouv.clea.identification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.mapper.SinglePlaceClusterPeriodMapper;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;

import fr.gouv.clea.dto.SinglePlaceCluster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

import javax.sql.DataSource;

import static fr.gouv.clea.config.BatchConstants.SINGLE_PLACE_CLUSTER_PERIOD_TABLE;

@Slf4j
public class SinglePlaceClusterPeriodListWriter extends JdbcBatchItemWriter<List<SinglePlaceClusterPeriod>> {

    private final SinglePlaceClusterPeriodMapper mapper;

    private final JdbcOperations jdbcTemplate;

    public SinglePlaceClusterPeriodListWriter(SinglePlaceClusterPeriodMapper mapper, DataSource datasource) {
        this.mapper = mapper;
        this.jdbcTemplate = new JdbcTemplate(datasource);
    }

    @Override
    public void write(List<? extends List<SinglePlaceClusterPeriod>> list) {
        list.get(0).forEach(singlePlaceClusterPeriod -> {
                    final BeanPropertySqlParameterSource parameterSource = new BeanPropertySqlParameterSource(singlePlaceClusterPeriod);
                    jdbcTemplate.update(getInsertSql(), parameterSource);
                });
    }

    private String getInsertSql() {
        return "insert into " + SINGLE_PLACE_CLUSTER_PERIOD_TABLE +
                // column names
                " (" +
                "ltid, " +
                "venue_type, " +
                "venue_category1, " +
                "venue_category2, " +
                "period_start, " +
                "first_time_slot, " +
                "last_time_slot, " +
                "cluster_start, " +
                "cluster_duration_in_seconds, " +
                "risk_level" +
                ")" +
                " values " +
                // values as parameters from SinglePlaceClusterPeriod attributes
                "(" +
                ":locationTemporaryPublicId, " +
                ":venueType, " +
                ":venueCategory1, " +
                ":venueCategory2, " +
                ":periodStart, " +
                ":firstTimeSlot, " +
                ":lastTimeSlot, " +
                ":clusterStart, " +
                ":clusterDurationInSeconds, " +
                ":riskLevel)";
    }
}
