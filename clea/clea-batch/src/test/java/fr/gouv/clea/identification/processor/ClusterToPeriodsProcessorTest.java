package fr.gouv.clea.identification.processor;

import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceClusterPeriod;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapper;
import fr.gouv.clea.mapper.ClusterPeriodModelsMapperImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

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

        final ClusterPeriod p1 = buildPeriod(clusterStart, clusterDurationInSeconds, firstTimeSlot, lastTimeSlot, riskLevel, periodStart);
        final SinglePlaceCluster cluster = buildCluster(ltid, venueType, venueCat1, venueCat2, p1);
        final List<SinglePlaceClusterPeriod> singlePlaceClusterPeriods = processor.process(cluster);

        Assertions.assertThat(singlePlaceClusterPeriods.size()).isEqualTo(1);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getClusterStart()).isEqualTo(clusterStart);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getClusterDurationInSeconds()).isEqualTo(clusterDurationInSeconds);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getFirstTimeSlot()).isEqualTo(firstTimeSlot);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getLastTimeSlot()).isEqualTo(lastTimeSlot);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getPeriodStart()).isEqualTo(periodStart);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getLocationTemporaryPublicId()).isEqualTo(ltid);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getVenueType()).isEqualTo(venueType);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getVenueCategory1()).isEqualTo(venueCat1);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getVenueCategory2()).isEqualTo(venueCat2);
        Assertions.assertThat(singlePlaceClusterPeriods.get(0).getRiskLevel()).isEqualTo(riskLevel);

    }

    private SinglePlaceCluster buildCluster(UUID ltid, int venueType, int venueCat1, int venueCat2, ClusterPeriod p1) {
        final SinglePlaceCluster cluster = new SinglePlaceCluster();
        cluster.setVenueType(venueType);
        cluster.setVenueCategory1(venueCat1);
        cluster.setVenueCategory2(venueCat2);
        cluster.setLocationTemporaryPublicId(ltid);
        cluster.setPeriods(List.of(p1));
        return cluster;
    }

    private ClusterPeriod buildPeriod(int clusterStart, int clusterDurationInSeconds, int firstTimeSlot, int lastTimeSlot, int riskLevel, long periodStart) {
        final ClusterPeriod p1 = new ClusterPeriod();
        p1.setClusterStart(clusterStart);
        p1.setClusterDurationInSeconds(clusterDurationInSeconds);
        p1.setFirstTimeSlot(firstTimeSlot);
        p1.setLastTimeSlot(lastTimeSlot);
        p1.setRiskLevel(riskLevel);
        p1.setPeriodStart(periodStart);
        return p1;
    }
}
