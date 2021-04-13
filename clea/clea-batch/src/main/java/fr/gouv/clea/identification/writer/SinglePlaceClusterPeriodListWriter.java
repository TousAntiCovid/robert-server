package fr.gouv.clea.identification.writer;

import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.clea.config.BatchConstants.*;

@Slf4j
@RequiredArgsConstructor
public class SinglePlaceClusterPeriodListWriter implements ItemWriter<List<SinglePlaceClusterPeriod>> {

    private final NamedParameterJdbcOperations jdbcTemplate;

    @Override
    public void write(List<? extends List<SinglePlaceClusterPeriod>> lists) {
        final List<SinglePlaceClusterPeriod> flatList = lists.stream().flatMap(List::stream).collect(Collectors.toList());
        final SqlParameterSource[] parameters = SqlParameterSourceUtils.createBatch(flatList);
        jdbcTemplate.batchUpdate(getInsertSql(), parameters);
    }

    private String getInsertSql() {
        // values as parameters from SinglePlaceClusterPeriod attributes
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
                ") values (" +
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
