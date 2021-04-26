package fr.gouv.clea.indexation.processor;

import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.indexation.SinglePlaceClusterPeriodRowMapper;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileItem;
import fr.gouv.clea.indexation.model.output.ExposureRow;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapper;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static fr.gouv.clea.config.BatchConstants.SQL_SELECT_BY_LTID_IN_SINGLEPLACECLUSTERPERIOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SinglePlaceClusterBuilderTest {

    @Captor
    private ArgumentCaptor<UUID> uuidArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> sqlStringArgumentCaptor;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ClusterPeriodModelsMapper mapper;

    @InjectMocks
    private SinglePlaceClusterBuilder processor;


    @Test
    void getSinglePLaceClusterPeriods_queries_jdbcTemplate_with_provided_ltid() {

        final String ltid = "729e373a-7ba3-4571-a5fc-c4591d6202cc";

        processor.getSinglePlaceClusterPeriods(ltid);

        verify(jdbcTemplate).query(sqlStringArgumentCaptor.capture(), any(SinglePlaceClusterPeriodRowMapper.class), uuidArgumentCaptor.capture());

        assertThat(uuidArgumentCaptor.getValue()).isEqualTo(UUID.fromString(ltid));
        assertThat(sqlStringArgumentCaptor.getValue()).isEqualTo(SQL_SELECT_BY_LTID_IN_SINGLEPLACECLUSTERPERIOD);
    }

    @Test
    void process_returns_clusterFile_with_prefixes_and_ltids_from_provided_list() {
        final String prefix = "f6";
        final UUID ltid1 = UUID.fromString("f67b2973-a1d5-4105-99d2-623262ced561");
        final UUID ltid2 = UUID.fromString("f67b2973-a1d5-4105-99d2-623262ced562");
        final List<String> ltidsList = List.of(ltid1.toString(), ltid2.toString());
        final Map.Entry<String, List<String>> input = new AbstractMap.SimpleEntry<>(prefix, ltidsList);
        final int venueCat1 = 0;
        final int venueCat2 = 1;
        final int venueType = 2;
        // POJOs concerning first ltid treatment
        final ClusterPeriod clusterPeriod1 = ClusterPeriod.builder().build();
        final SinglePlaceClusterPeriod period1 = buildSinglePlaceClusterPeriod(ltid1, venueCat1, venueCat2, venueType);
        // POJOs concerning second ltid treatment
        final ClusterPeriod clusterPeriod2 = ClusterPeriod.builder().build();
        final SinglePlaceClusterPeriod period2 = buildSinglePlaceClusterPeriod(ltid2, venueCat1, venueCat2, venueType);

        when(jdbcTemplate.query(eq(SQL_SELECT_BY_LTID_IN_SINGLEPLACECLUSTERPERIOD), any(SinglePlaceClusterPeriodRowMapper.class), eq(ltid1))).thenReturn(List.of(period1));
        when(jdbcTemplate.query(eq(SQL_SELECT_BY_LTID_IN_SINGLEPLACECLUSTERPERIOD), any(SinglePlaceClusterPeriodRowMapper.class), eq(ltid2))).thenReturn(List.of(period2));
        when(mapper.map(period1)).thenReturn(clusterPeriod1);
        when(mapper.map(period2)).thenReturn(clusterPeriod2);

        final ClusterFile result = processor.process(input);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(prefix);
        final ClusterFileItem expectedItem1 = buildClusterFileItem(ltid1, venueCat1, venueCat2, venueType, List.of(clusterPeriod1));
        final ClusterFileItem expectedItem2 = buildClusterFileItem(ltid2, venueCat1, venueCat2, venueType, List.of(clusterPeriod2));
        assertThat(result.getItems()).containsExactly(expectedItem1, expectedItem2);
    }

    @Test
    void createClusterFileItem_returns_ClusterFileItem_from_input_periods() {
        final UUID ltid1 = UUID.fromString("f67b2973-a1d5-4105-99d2-623262ced561");
        final int venueCat1 = 0;
        final int venueCat2 = 1;
        final int venueType = 2;
        final int startTimestamp1 = 3;
        final int startTimestamp2 = 4;
        final long duration1 = 0L;
        final long duration2 = 1L;
        final float riskLevel1 = 0;
        final float riskLevel2 = 1;
        final ClusterPeriod clusterPeriod1 = ClusterPeriod.builder()
                .clusterStart(startTimestamp1)
                .clusterDurationInSeconds((int) duration1)
                .riskLevel(riskLevel1)
                .build();
        final ClusterPeriod clusterPeriod2 = ClusterPeriod.builder()
                .clusterStart(startTimestamp2)
                .clusterDurationInSeconds((int) duration2)
                .riskLevel(riskLevel2)
                .build();

        final SinglePlaceClusterPeriod period1 = buildSinglePlaceClusterPeriod(ltid1, venueCat1, venueCat2, venueType);

        final ClusterFileItem result = processor.createClusterFileItem(period1, List.of(clusterPeriod1, clusterPeriod2));

        assertThat(result.getTemporaryLocationId()).isEqualTo(ltid1.toString());
        final var expectedExposureRow1 = buildExposureRow(startTimestamp1, duration1, riskLevel1);
        final var expectedExposureRow2 = buildExposureRow(startTimestamp2, duration2, riskLevel2);
        assertThat(result.getExposures()).containsExactly(expectedExposureRow1, expectedExposureRow2);
    }

    private ExposureRow buildExposureRow(int startTimestamp1, long duration1, float riskLevel1) {
        return ExposureRow.builder()
                .startTimestamp(startTimestamp1)
                .durationInSeconds(duration1)
                .riskLevel(riskLevel1)
                .build();
    }

    private SinglePlaceClusterPeriod buildSinglePlaceClusterPeriod(UUID ltid2, int venueCat1, int venueCat2, int venueType) {
        return SinglePlaceClusterPeriod.builder()
                .locationTemporaryPublicId(ltid2)
                .venueCategory1(venueCat1)
                .venueCategory2(venueCat2)
                .venueType(venueType)
                .build();
    }

    private ClusterFileItem buildClusterFileItem(UUID ltid1, int venueCat1, int venueCat2, int venueType, List<ClusterPeriod> clusterPeriods) {
        return ClusterFileItem.ofCluster(SinglePlaceCluster.builder()
                .locationTemporaryPublicId(ltid1)
                .venueCategory1(venueCat1)
                .venueCategory2(venueCat2)
                .venueType(venueType)
                .periods(clusterPeriods)
                .build());
    }
}
