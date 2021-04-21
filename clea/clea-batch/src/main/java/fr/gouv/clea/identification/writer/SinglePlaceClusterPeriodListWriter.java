package fr.gouv.clea.identification.writer;

import static fr.gouv.clea.config.BatchConstants.CLUSTER_DURATION_COL;
import static fr.gouv.clea.config.BatchConstants.CLUSTER_START_COL;
import static fr.gouv.clea.config.BatchConstants.FIRST_TIMESLOT_COL;
import static fr.gouv.clea.config.BatchConstants.LAST_TIMESLOT_COL;
import static fr.gouv.clea.config.BatchConstants.LTID_COL;
import static fr.gouv.clea.config.BatchConstants.PERIOD_START_COL;
import static fr.gouv.clea.config.BatchConstants.RISK_LEVEL_COL;
import static fr.gouv.clea.config.BatchConstants.SINGLE_PLACE_CLUSTER_PERIOD_TABLE;
import static fr.gouv.clea.config.BatchConstants.VENUE_CAT1_COL;
import static fr.gouv.clea.config.BatchConstants.VENUE_CAT2_COL;
import static fr.gouv.clea.config.BatchConstants.VENUE_TYPE_COL;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;

import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import lombok.RequiredArgsConstructor;

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
