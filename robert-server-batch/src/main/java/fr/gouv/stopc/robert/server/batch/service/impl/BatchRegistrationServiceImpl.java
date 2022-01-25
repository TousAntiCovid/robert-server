package fr.gouv.stopc.robert.server.batch.service.impl;

import fr.gouv.stopc.robert.server.batch.service.BatchRegistrationService;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class BatchRegistrationServiceImpl implements BatchRegistrationService {

    private final ScoringStrategyService scoringStrategy;

    /**
     * Keep epochs within the contagious period
     */
    @Override
    public List<EpochExposition> getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
            final List<EpochExposition> exposedEpochs,
            final int currentEpochId,
            final int contagiousPeriod,
            final int epochDuration) {

        // Purge exposed epochs list from epochs older than contagious period (C_T)
        return CollectionUtils.isEmpty(exposedEpochs) ? new ArrayList<>()
                : exposedEpochs.stream().filter(epoch -> {
                    final var nbOfEpochsToKeep = (contagiousPeriod * 24 * 3600) / epochDuration;
                    return (currentEpochId - epoch.getEpochId()) <= nbOfEpochsToKeep;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean updateRegistrationIfRisk(final Registration registration,
            final long serviceTimeStart,
            final double riskThreshold) {
        boolean isRegistrationAtRisk = false;
        final var latestRiskEpoch = registration.getLatestRiskEpoch();
        final var epochExpositions = registration.getExposedEpochs();

        // Only consider epochs that are after the last notification for scoring
        final List<EpochExposition> scoresSinceLastNotif = CollectionUtils.isEmpty(epochExpositions) ? new ArrayList<>()
                : epochExpositions.stream()
                        .filter(ep -> ep.getEpochId() > latestRiskEpoch)
                        .collect(Collectors.toList());

        // Create a single list with all contact scores from all epochs
        final var allScoresFromAllEpochs = scoresSinceLastNotif.stream()
                .map(EpochExposition::getExpositionScores)
                .map(item -> item.stream().mapToDouble(Double::doubleValue).sum())
                .collect(Collectors.toList());

        final var totalRisk = scoringStrategy.aggregate(allScoresFromAllEpochs);
        ;

        if (totalRisk >= riskThreshold && lastContactDateIsBeforePivotDate(registration)) {
            log.info(
                    "Risk detected. Aggregated risk since {}: {} greater than threshold {}",
                    latestRiskEpoch,
                    totalRisk,
                    riskThreshold
            );

            scoresSinceLastNotif.stream()
                    .max(Comparator.comparing(EpochExposition::getEpochId))
                    .ifPresent(lastContactEpoch -> {
                        final var lastContactTimestamp = TimeUtils
                                .getNtpSeconds(lastContactEpoch.getEpochId(), serviceTimeStart);
                        final var randomizedLastContactTimestamp = TimeUtils.dayTruncatedTimestamp(
                                TimeUtils.getRandomizedDateNotInFuture(lastContactTimestamp)
                        );
                        if (randomizedLastContactTimestamp > registration.getLastContactTimestamp()) {
                            log.debug(
                                    "Last contact date is updating : last contact date from hello message : {}" +
                                            " - previous last contact date : {} ==> stored last contact date : {}  ",
                                    lastContactTimestamp,
                                    registration.getLastContactTimestamp(),
                                    randomizedLastContactTimestamp
                            );
                            registration.setLastContactTimestamp(randomizedLastContactTimestamp);
                        } else {
                            log.debug(
                                    "Last contact date isn't updating : last contact date from hello message : {} - randomized to {}"
                                            +
                                            " - previous last contact date : {}",
                                    lastContactTimestamp,
                                    randomizedLastContactTimestamp,
                                    registration.getLastContactTimestamp()
                            );
                        }
                    });

            // A risk has been detected, move time marker to now so that further risks are
            // only posterior to this one
            final var newLatestRiskEpoch = TimeUtils.getCurrentEpochFrom(serviceTimeStart);
            registration.setLatestRiskEpoch(newLatestRiskEpoch);
            log.info("Updating latest risk epoch {}", newLatestRiskEpoch);
            registration.setAtRisk(true);
            // Do not reset isNotified since it is used to compute the number of
            // notifications
            // It is up to the client to know if it should notify (new risk) or not given
            // the risk change or not.
            isRegistrationAtRisk = true;
        }

        return isRegistrationAtRisk;
    }

    private boolean lastContactDateIsBeforePivotDate(Registration registration) {

        final var lastContactDateTimeZone = Instant.ofEpochMilli(
                TimeUtils.convertNTPSecondsToUnixMillis(registration.getLastContactTimestamp())
        );

        return Duration.between(lastContactDateTimeZone, Instant.now()).abs().toDays() <= 7;
    }
}
