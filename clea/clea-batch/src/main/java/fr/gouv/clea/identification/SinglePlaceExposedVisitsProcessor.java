package fr.gouv.clea.identification;

import fr.gouv.clea.clea.scoring.configuration.domain.risk.RiskRule;
import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.dto.ClusterPeriod;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.dto.SinglePlaceExposedVisits;
import fr.gouv.clea.entity.ExposedVisit;
import fr.gouv.clea.utils.ExposedVisitComparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.Optional;

@Slf4j
public class SinglePlaceExposedVisitsProcessor implements ItemProcessor<SinglePlaceExposedVisits, SinglePlaceCluster> {

    private final BatchProperties properties;
    private final RiskConfigurationService riskConfigurationService;

    public SinglePlaceExposedVisitsProcessor(BatchProperties properties, RiskConfigurationService riskConfigurationService) {
        this.properties = properties;
        this.riskConfigurationService = riskConfigurationService;
    }

    @Override
    public SinglePlaceCluster process(SinglePlaceExposedVisits record) {
        SinglePlaceCluster cluster = new SinglePlaceCluster();

        cluster.setLocationTemporaryPublicId(record.getLocationTemporaryPublicId());
        cluster.setVenueType(record.getVenueType());
        cluster.setVenueCategory1(record.getVenueCategory1());
        cluster.setVenueCategory2(record.getVenueCategory2());

        ClusterPeriod backPeriod = null;
        ClusterPeriod forwardPeriod = null;

        Optional<RiskRule> riskRuleOptional = riskConfigurationService.evaluate(cluster.getVenueType(), cluster.getVenueCategory1(), cluster.getVenueCategory2());
        if (riskRuleOptional.isEmpty()) {
            log.warn("No Risk configuration for [type={},categ1={},categ2={}]", cluster.getVenueType(), cluster.getVenueCategory1(), cluster.getVenueCategory2());
            return null;
        }
        final RiskRule riskRule = riskRuleOptional.get();

        // Sorted visits by period then slot
        record.getVisits().sort(new ExposedVisitComparator());

        for (ExposedVisit visit : record.getVisits()) {
            // Backward
            backPeriod = processVisit(visit, cluster, backPeriod, visit.getBackwardVisits(),
                    riskRule.getClusterThresholdBackward(), riskRule.getRiskLevelBackward());
            // Forward
            forwardPeriod = processVisit(visit, cluster, forwardPeriod, visit.getForwardVisits(), 
                    riskRule.getClusterThresholdForward(), riskRule.getRiskLevelForward());
        }

        // Finalize last periods after the loop
        if (!noCurrentCluster(backPeriod)) {
            cluster.addPeriod(backPeriod);
        }
        if (!noCurrentCluster(forwardPeriod)) {
            cluster.addPeriod(forwardPeriod);
        }

        cluster.computeDurations(properties.durationUnitInSeconds);

        return cluster.isEmpty() ? null : cluster;
    }

    protected ClusterPeriod processVisit(ExposedVisit visit, SinglePlaceCluster cluster,
            ClusterPeriod period, long nbVisits, int threshold, float risk) {
        if (isACluster(nbVisits, threshold)) {
            // We detect that this visit is a cluster,
            // 3 possible use-case:
            // 1°) no current cluster, start one.
            // 2°) existing cluster with same startPeriod : check previous slotIndex.
            // 3°) existing cluster with different startPeriod => finalize the previous, create new one

            if (noCurrentCluster(period)) {
                period = initClusterPeriod(visit, risk);
            } else if (period.isInSameCluster(visit)) {
                period.adjustLimit(visit);
            } else {
                cluster.addPeriod(period);
                // Init a new period because we know that the visit is in a cluster
                period = initClusterPeriod(visit, risk);
            }
        } else if (!noCurrentCluster(period)) {
            cluster.addPeriod(period);
            period = null;
        }
        return period;
    }

    private ClusterPeriod initClusterPeriod(ExposedVisit v, float riskLevel) {
        return ClusterPeriod.builder()
                .periodStart(v.getPeriodStart())
                .firstTimeSlot(v.getTimeSlot())
                .lastTimeSlot(v.getTimeSlot())
                .riskLevel(riskLevel)
                .build();
    }

    private boolean noCurrentCluster(ClusterPeriod cluster) {
        return null == cluster;
    }

    protected boolean isACluster(long nbExpositions, int threshold) {
        return nbExpositions >= threshold;
    }

}
