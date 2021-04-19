package fr.gouv.clea.identification;

import fr.gouv.clea.clea.scoring.configuration.domain.risk.RiskRule;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.entity.ExposedVisit;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SinglePlaceExposedVisitsProcessorTest {

	private BatchProperties properties=new BatchProperties();
	RiskConfigurationService eval = new RiskConfigurationService();
	
	public SinglePlaceExposedVisitsProcessorTest() {
		properties.durationUnitInSeconds =180;
	}

	private final UUID UUID_SAMPLE = UUID.fromString("fa35fa88-2c44-4f13-9ec9-d38e77324c93");
	private final long periodStart = 3822336080L;

	@Test
	void noClusterPeriod() {
		SinglePlaceExposedVisits spe = new SinglePlaceExposedVisits();
		spe.setLocationTemporaryPublicId(UUID_SAMPLE);
		spe.setVenueType(18);
		spe.setVenueCategory1(3);
		spe.setVenueCategory2(2);
				
		SinglePlaceCluster res = new SinglePlaceExposedVisitsProcessor(properties,eval).process(spe);
		assertThat(res).isNull();
	}
	
	@Test
	void noClusterVisits() throws Exception {
		SinglePlaceExposedVisits spe = new SinglePlaceExposedVisits();
		spe.setLocationTemporaryPublicId(UUID_SAMPLE);
		spe.setVenueType(18);
		spe.setVenueCategory1(3);
		spe.setVenueCategory2(2);
		
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(0).forwardVisits(0).build());
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(1).forwardVisits(0).build());

		SinglePlaceCluster res = new SinglePlaceExposedVisitsProcessor(properties,eval).process(spe);
		assertThat(res).isNull();
		
	}	

	@Test
	void oneClusterPeriod() {
		SinglePlaceExposedVisits spe = new SinglePlaceExposedVisits();
		spe.setLocationTemporaryPublicId(UUID_SAMPLE);
		spe.setVenueType(18);
		spe.setVenueCategory1(3);
		spe.setVenueCategory2(2);
		
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(0).forwardVisits(0).build());
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(1).forwardVisits(1).build());
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(2).forwardVisits(1).build());
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(3).forwardVisits(0).build());
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(4).forwardVisits(0).build());
		

		SinglePlaceCluster res = new SinglePlaceExposedVisitsProcessor(properties,eval).process(spe);
		assertThat(res).isNotNull();
		assertThat(res.getPeriods()).hasSize(1);
		
		ClusterPeriod p = res.getPeriods().get(0);
		// cluster start at slot 1, not at slot 0
		assertThat(p.getClusterStart()).as("clusterStart").isEqualTo(periodStart + properties.durationUnitInSeconds);
		// Cluster for 2 slots
		assertThat(p.getClusterDurationInSeconds()).as("clusterDuration").isEqualTo(2* properties.durationUnitInSeconds);
		
	}
	
	@Test
	void manyClusterPeriodSlots() {
		long anotherPeriodStart = 3822422480L;

		SinglePlaceExposedVisits spe = new SinglePlaceExposedVisits();
		spe.setLocationTemporaryPublicId(UUID_SAMPLE);
		spe.setVenueType(18);
		spe.setVenueCategory1(3);
		spe.setVenueCategory2(2);

		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(0).forwardVisits(0).build());
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(1).forwardVisits(1).build());
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(2).forwardVisits(0).build());
		spe.addVisit(ExposedVisit.builder().periodStart(anotherPeriodStart).timeSlot(0).forwardVisits(1).build());
		
		SinglePlaceCluster res = new SinglePlaceExposedVisitsProcessor(properties,eval).process(spe);
		assertThat(res).isNotNull();
		assertThat(res.getPeriods()).hasSize(2);

		ClusterPeriod p = res.getPeriods().get(0);
		// cluster start at slot 1, not at slot 0
		assertThat(p.getClusterStart()).as("clusterStart").isEqualTo(this.periodStart + properties.durationUnitInSeconds);
		assertThat(p.getClusterDurationInSeconds()).as("clusterDuration").isEqualTo(1* properties.durationUnitInSeconds);

		p = res.getPeriods().get(1);
		// cluster start at slot 0, not at slot 1
		assertThat(p.getClusterStart()).as("clusterStart").isEqualTo(anotherPeriodStart);
		assertThat(p.getClusterDurationInSeconds()).as("clusterDuration").isEqualTo(1* properties.durationUnitInSeconds);
	}

	
	
	
	@Test
	void forwardRiskLevel() {
		SinglePlaceExposedVisits spe = new SinglePlaceExposedVisits();
		spe.setLocationTemporaryPublicId(UUID_SAMPLE);
		spe.setVenueType(18);
		spe.setVenueCategory1(3);
		spe.setVenueCategory2(2);
		
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(0).forwardVisits(100).build());
		SinglePlaceCluster res = new SinglePlaceExposedVisitsProcessor(properties,eval).process(spe);
		assertThat(res).isNotNull();
		assertThat(res.getPeriods()).hasSize(1);

		ClusterPeriod p = res.getPeriods().get(0);
		Optional<RiskRule> riskLevelEvaluation = eval.evaluate(spe.getVenueType(), spe.getVenueCategory1(), spe.getVenueCategory2());
		riskLevelEvaluation.ifPresent(evaluatedRiskLevel -> assertThat(p.getRiskLevel()).as("riskLevel").isCloseTo(evaluatedRiskLevel.getRiskLevelForward(), Offset.offset(0.01f)));
	}

	@Test
	void backwardRiskLevel() {
		SinglePlaceExposedVisits spe = new SinglePlaceExposedVisits();
		spe.setLocationTemporaryPublicId(UUID_SAMPLE);
		spe.setVenueType(18);
		spe.setVenueCategory1(3);
		spe.setVenueCategory2(2);
		
		spe.addVisit(ExposedVisit.builder().periodStart(periodStart).timeSlot(0).backwardVisits(100).build());
		SinglePlaceCluster res = new SinglePlaceExposedVisitsProcessor(properties,eval).process(spe);
		assertThat(res).isNotNull();
		assertThat(res.getPeriods()).hasSize(1);

		ClusterPeriod p = res.getPeriods().get(0);
		Optional<RiskRule> riskLevelEvaluation = eval.evaluate(spe.getVenueType(), spe.getVenueCategory1(), spe.getVenueCategory2());
		riskLevelEvaluation.ifPresent(evaluatedRiskLevel -> assertThat(p.getRiskLevel()).as("riskLevel").isCloseTo(evaluatedRiskLevel.getRiskLevelBackward(), Offset.offset(0.01f)));
	}

}
