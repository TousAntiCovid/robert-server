package fr.gouv.clea.identification.writer;

import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SinglePlaceClusterPeriodListWriterTest {

    @Captor
    private ArgumentCaptor<SqlParameterSource[]> sqlParameterSourcesCaptor;

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    SinglePlaceClusterPeriodListWriter writer;

    @Test
    void write_calls_template_with_parameters_from_provided_list() {
        final UUID ltid1 = UUID.randomUUID();
        final int venueType1 = 0;
        final int venueCat1_1 = 1;
        final int venueCat2_1 = 2;
        final long periodStart1 = 0L;
        final int firstTS1 = 3;
        final int lastTS1 = 4;
        final long clusterStart1 = 1L;
        final int clusterDuration1 = 5;
        final float riskLv1 = 1;
        final UUID ltid2 = UUID.randomUUID();
        final int venueType2 = 6;
        final int venueCat1_2 = 7;
        final int venueCat2_2 = 8;
        final long periodStart2 = 0L;
        final int firstTS2 = 9;
        final int lastTS2 = 10;
        final long clusterStart2 = 1L;
        final int clusterDuration2 = 11;
        final float riskLv2 = 1;
        final SinglePlaceClusterPeriod period1 = buildClusterPeriod(ltid1, venueType1, venueCat1_1, venueCat2_1, periodStart1, firstTS1, lastTS1, clusterStart1, clusterDuration1, riskLv1);
        final SinglePlaceClusterPeriod period2 = buildClusterPeriod(ltid2, venueType2, venueCat1_2, venueCat2_2, periodStart2, firstTS2, lastTS2, clusterStart2, clusterDuration2, riskLv2);

        when(jdbcTemplate.batchUpdate(anyString(), sqlParameterSourcesCaptor.capture())).thenReturn(new int[2]);

        // two lists of two singlePlaceClusterPeriods as input, one per ltid
        writer.write(List.of(List.of(period1), List.of(period2)));

        sqlParameterSourcesCaptor.getAllValues().forEach(sqlParameterSources -> log.info("sqlParameterSource: {}", Arrays.toString(sqlParameterSources)));
    }

    private SinglePlaceClusterPeriod buildClusterPeriod(final UUID ltid,
                                                        final int venueType,
                                                        final int venueCat1,
                                                        final int venueCat2,
                                                        final long periodStart,
                                                        final int firstTS,
                                                        final int lastTS,
                                                        final long clusterStart,
                                                        final int clusterDuration,
                                                        final float riskLv) {
        return SinglePlaceClusterPeriod.builder()
                .locationTemporaryPublicId(ltid)
                .venueType(venueType)
                .venueCategory1(venueCat1)
                .venueCategory2(venueCat2)
                .periodStart(periodStart)
                .firstTimeSlot(firstTS)
                .lastTimeSlot(lastTS)
                .clusterStart(clusterStart)
                .clusterDurationInSeconds(clusterDuration)
                .riskLevel(riskLv)
                .build();
    }
}
