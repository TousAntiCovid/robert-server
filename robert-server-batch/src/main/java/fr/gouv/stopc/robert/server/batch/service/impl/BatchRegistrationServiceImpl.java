package fr.gouv.stopc.robert.server.batch.service.impl;

import fr.gouv.stopc.robert.server.batch.service.BatchRegistrationService;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.service.RobertClock.RobertInstant;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

@Slf4j
@Service
@AllArgsConstructor
public class BatchRegistrationServiceImpl implements BatchRegistrationService {

    private final ScoringStrategyService scoringStrategy;

    private final PropertyLoader propertyLoader;

    private final RobertClock robertClock;

    /**
     * Keep epochs within the contagious period
     */
    @Override
    public List<EpochExposition> getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(
            List<EpochExposition> exposedEpochs,
            int currentEpochId,
            int contagiousPeriod,
            int epochDuration) {

        // Purge exposed epochs list from epochs older than contagious period (C_T)
        return CollectionUtils.isEmpty(exposedEpochs) ? new ArrayList<>()
                : exposedEpochs.stream().filter(epoch -> {
                    int nbOfEpochsToKeep = (contagiousPeriod * 24 * 3600) / epochDuration;
                    return (currentEpochId - epoch.getEpochId()) <= nbOfEpochsToKeep;
                }).collect(Collectors.toList());
    }

    @Override
    public boolean updateRegistrationIfRisk(Registration registration,
            long serviceTimeStart,
            double riskThreshold) {
        boolean isRegistrationAtRisk = false;
        int latestRiskEpoch = registration.getLatestRiskEpoch();
        List<EpochExposition> epochExpositions = registration.getExposedEpochs();

        // Only consider epochs that are after the last notification for scoring
        List<EpochExposition> scoresSinceLastNotif = CollectionUtils.isEmpty(epochExpositions) ? new ArrayList<>()
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
            log.info(
                    "Risk detected. Aggregated risk since {}: {} greater than threshold {}",
                    latestRiskEpoch,
                    totalRisk,
                    riskThreshold
            );

            scoresSinceLastNotif.stream()
                    .mapToInt(EpochExposition::getEpochId)
                    .max()
                    .ifPresent(lastContactEpoch -> {

                        final var lastContactTime = robertClock.atEpoch(lastContactEpoch);
                        final var randomLastContactTime = randomizePlusOrMinusOneDay(lastContactTime);
                        final var actualRegistrationLastContact = robertClock
                                .atNtpTimestamp(registration.getLastContactTimestamp());

                        if (randomLastContactTime.isAfter(robertClock.now())) {
                            log.warn("last contact exposition is in the future, setting lastContactDate to today");
                            final var today = robertClock.now().truncatedTo(DAYS);
                            registration.setLastContactTimestamp(today.asNtpTimestamp());
                        } else if (randomLastContactTime.isAfter(actualRegistrationLastContact)) {
                            log.debug(
                                    "updating last contact date with randomized value: {} -> {}", lastContactTime,
                                    randomLastContactTime
                            );
                            registration.setLastContactTimestamp(randomLastContactTime.asNtpTimestamp());
                        } else {
                            log.debug(
                                    "keeping previous last contact date {} because is was older than randomized value {}",
                                    lastContactTime, randomLastContactTime
                            );
                        }
                    });

            // A risk has been detected, move time marker to now so that further risks are
            // only posterior to this one
            int newLatestRiskEpoch = TimeUtils.getCurrentEpochFrom(serviceTimeStart);
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

    private RobertInstant randomizePlusOrMinusOneDay(final RobertInstant lastContactTime) {
        final var dayCountFromLastContact = lastContactTime.until(robertClock.now())
                .toDays();
        final int pastBound;
        final int futureBound;
        // comment assumes the risk level retention is 7 days
        if (dayCountFromLastContact == 0) {
            // take care to generate a random date between yesterday and today
            pastBound = -1;
            futureBound = 0;
        } else if (dayCountFromLastContact == propertyLoader.getRiskLevelRetentionPeriodInDays()) {
            // take care to generate a random date between 7 days ago and 6 days ago
            pastBound = 0;
            futureBound = 1;
        } else if (dayCountFromLastContact == propertyLoader.getRiskLevelRetentionPeriodInDays() + 1) {
            // take care to generate a random date between 9 days ago and 8 days ago
            pastBound = -1;
            futureBound = 0;
        } else {
            // generate a random date 1 day around last contact date
            pastBound = -1;
            futureBound = 1;
        }
        final var random = ThreadLocalRandom.current().nextInt(pastBound, futureBound + 1);
        return lastContactTime
                .plus(random, DAYS)
                .truncatedTo(DAYS);
    }
}
