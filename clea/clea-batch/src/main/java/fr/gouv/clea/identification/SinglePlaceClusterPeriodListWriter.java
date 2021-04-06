package fr.gouv.clea.identification;

import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.mapper.SinglePlaceClusterPeriodMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static fr.gouv.clea.config.BatchConstants.*;

@Slf4j
public class SinglePlaceClusterPeriodListWriter extends JdbcBatchItemWriter<List<SinglePlaceClusterPeriod>> {

    private final SinglePlaceClusterPeriodMapper mapper;

    private final NamedParameterJdbcOperations jdbcTemplate;

    public SinglePlaceClusterPeriodListWriter(SinglePlaceClusterPeriodMapper mapper, DataSource datasource) {
        this.mapper = mapper;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(datasource);
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
                String.join(", ", LTID_COL,
                        VENUE_TYPE_COL,
                        VENUE_CAT1_COL,
                        VENUE_CAT2_COL,
                        PERIOD_START_COL,
                        FIRST_TIMESLOT_COL,
                        LAST_TIMESLOT_COL,
                        CLUSTER_START_COL,
                        CLUSTER_DURATION_COL,
                        RISK_LEVEL_COL) +
                ")" +
                " values " +
                // values as parameters from SinglePlaceClusterPeriod attributes
                "(" +
                String.join(", ",
                        ":locationTemporaryPublicId",
                        ":venueType", ":venueCategory1",
                        ":venueCategory2",
                        ":periodStart",
                        ":firstTimeSlot",
                        ":lastTimeSlot",
                        ":clusterStart",
                        ":clusterDurationInSeconds",
                        ":riskLevel")
                + ")";
    }
}
