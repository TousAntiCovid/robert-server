package fr.gouv.clea.identification.processor;

import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapper;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapperImpl;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ClusterToPeriodsProcessorTest {

    private final ClusterPeriodModelsMapper mapper = new ClusterPeriodModelsMapperImpl();

    private final ClusterToPeriodsProcessor processor = new ClusterToPeriodsProcessor(mapper);

    @Test
    void process_returns_list_of_singlePlace() {
        final UUID ltid = UUID.randomUUID();
        final int venueType = 1;
        final int venueCat1 = 1;
        final int venueCat2 = 2;
        final int clusterStart = 1;
        final int clusterDurationInSeconds = 1000;
        final int firstTimeSlot = 1;
        final int lastTimeSlot = 1001;
        final int riskLevel = 4;
        final long periodStart = 1L;
        final ClusterPeriod clusterPeriod = buildPeriod(clusterStart, clusterDurationInSeconds, firstTimeSlot, lastTimeSlot, riskLevel, periodStart);
        final SinglePlaceCluster cluster = buildCluster(ltid, venueType, venueCat1, venueCat2, clusterPeriod);

        final List<SinglePlaceClusterPeriod> singlePlaceClusterPeriods = processor.process(cluster);

        assertThat(singlePlaceClusterPeriods.size()).isEqualTo(1);
        assertThat(singlePlaceClusterPeriods.get(0).getClusterStart()).isEqualTo(clusterStart);
        assertThat(singlePlaceClusterPeriods.get(0).getClusterDurationInSeconds()).isEqualTo(clusterDurationInSeconds);
        assertThat(singlePlaceClusterPeriods.get(0).getFirstTimeSlot()).isEqualTo(firstTimeSlot);
        assertThat(singlePlaceClusterPeriods.get(0).getLastTimeSlot()).isEqualTo(lastTimeSlot);
        assertThat(singlePlaceClusterPeriods.get(0).getPeriodStart()).isEqualTo(periodStart);
        assertThat(singlePlaceClusterPeriods.get(0).getLocationTemporaryPublicId()).isEqualTo(ltid);
        assertThat(singlePlaceClusterPeriods.get(0).getVenueType()).isEqualTo(venueType);
        assertThat(singlePlaceClusterPeriods.get(0).getVenueCategory1()).isEqualTo(venueCat1);
        assertThat(singlePlaceClusterPeriods.get(0).getVenueCategory2()).isEqualTo(venueCat2);
        assertThat(singlePlaceClusterPeriods.get(0).getRiskLevel()).isEqualTo(riskLevel);

    }

    private SinglePlaceCluster buildCluster(final UUID ltid, final int venueType, final int venueCat1, final int venueCat2, final ClusterPeriod p1) {
        final SinglePlaceCluster cluster = new SinglePlaceCluster();
        cluster.setVenueType(venueType);
        cluster.setVenueCategory1(venueCat1);
        cluster.setVenueCategory2(venueCat2);
        cluster.setLocationTemporaryPublicId(ltid);
        cluster.setPeriods(List.of(p1));
        return cluster;
    }

    private ClusterPeriod buildPeriod(final int clusterStart, final int clusterDurationInSeconds, final int firstTimeSlot, final int lastTimeSlot, final int riskLevel, final long periodStart) {
        final ClusterPeriod clusterPeriod = new ClusterPeriod();
        clusterPeriod.setClusterStart(clusterStart);
        clusterPeriod.setClusterDurationInSeconds(clusterDurationInSeconds);
        clusterPeriod.setFirstTimeSlot(firstTimeSlot);
        clusterPeriod.setLastTimeSlot(lastTimeSlot);
        clusterPeriod.setRiskLevel(riskLevel);
        clusterPeriod.setPeriodStart(periodStart);
        return clusterPeriod;
    }
}
