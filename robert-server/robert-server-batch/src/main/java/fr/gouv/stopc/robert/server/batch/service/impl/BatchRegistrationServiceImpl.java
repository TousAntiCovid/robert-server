package fr.gouv.stopc.robert.server.batch.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class BatchRegistrationServiceImpl {


    private ScoringStrategyService scoringStrategy;

    /**
     * Keep epochs within the contagious period
     * @param exposedEpochs
     * @return
     */
    public List<EpochExposition> getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
            List<EpochExposition> exposedEpochs,
            int currentEpochId,
            int contagiousPeriod,
            int epochDuration) {

        // Purge exposed epochs list from epochs older than contagious period (C_T)
        return CollectionUtils.isEmpty(exposedEpochs) ?
                new ArrayList<>()
                : exposedEpochs.stream().filter(epoch -> {
            int nbOfEpochsToKeep = (contagiousPeriod * 24 * 3600) / epochDuration;
            return (currentEpochId - epoch.getEpochId()) <= nbOfEpochsToKeep;
        }).collect(Collectors.toList());
    }

    public boolean updateRegistrationIfRisk(Registration registration,
                                                long timeStart,
                                                double riskThreshold) {
        boolean isRegistrationAtRisk = false;
        int latestRiskEpoch = registration.getLatestRiskEpoch();
        List<EpochExposition> epochExpositions = registration.getExposedEpochs();

        // Only consider epochs that are after the last notification for scoring
        List<EpochExposition> scoresSinceLastNotif = CollectionUtils.isEmpty(epochExpositions) ?
                new ArrayList<>()
                : epochExpositions.stream()
                .filter(ep -> ep.getEpochId() > latestRiskEpoch)
                .collect(Collectors.toList());

        // Create a single list with all contact scores from all epochs
        List<Double> allScoresFromAllEpochs = scoresSinceLastNotif.stream()
                .map(EpochExposition::getExpositionScores)
                .map(item -> item.stream().mapToDouble(Double::doubleValue).sum())
                .collect(Collectors.toList());

        Double totalRisk = scoringStrategy.aggregate(allScoresFromAllEpochs);

        if (totalRisk >= riskThreshold) {
            log.info("Risk detected. Aggregated risk since {}: {} greater than threshold {}",
                    latestRiskEpoch,
                    totalRisk,
                    riskThreshold);

            // A risk has been detected, move time marker to now so that further risks are only posterior to this one
            int newLatestRiskEpoch = TimeUtils.getCurrentEpochFrom(timeStart);
            registration.setLatestRiskEpoch(newLatestRiskEpoch);
            log.info("Updating latest risk epoch {}", newLatestRiskEpoch);
            registration.setAtRisk(true);
            // Do not reset isNotified since it is used to compute the number of notifications
            // It is up to the client to know if it should notify (new risk) or not given the risk change or not.
            isRegistrationAtRisk = true;
        }

        return isRegistrationAtRisk;
    }
}
