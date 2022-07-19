package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.api.v1.model.RobertServerKpiV1;
import fr.gouv.stopc.robertserver.ws.api.v2.model.RobertServerKpiV2;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.*;
import static org.springframework.data.domain.Range.Bound.exclusive;
import static org.springframework.data.domain.Range.Bound.inclusive;

@Slf4j
@Service
public class KpiService {

    private final IRegistrationService registrationDbService;

    private final WebserviceStatisticsRepository webserviceStatisticsRepository;

    private final BatchStatisticsRepository batchStatisticsRepository;

    private Instant lastKpiComputationInstant = Instant.now();

    private final Gauge lastKpiComputationAgeGauge;

    public KpiService(final IRegistrationService registrationDbService,
            final WebserviceStatisticsRepository webserviceStatisticsRepository,
            final BatchStatisticsRepository batchStatisticsRepository,
            final MeterRegistry meterRegistry) {
        this.registrationDbService = registrationDbService;
        this.webserviceStatisticsRepository = webserviceStatisticsRepository;
        this.batchStatisticsRepository = batchStatisticsRepository;
        lastKpiComputationAgeGauge = Gauge
                .builder(
                        "robert.ws.dailystatistics.lastsuccess.age",
                        () -> Duration.between(lastKpiComputationInstant, now()).toSeconds()
                )
                .description("age of the last kpi computation success")
                .baseUnit("seconds")
                .register(meterRegistry);
    }

    public void computeDailyKpis() {
        log.info("starting KPIs computation");
        try {
            final var nbAlertedUsers = registrationDbService.countNbUsersNotified();
            final var nbExposedUsersNotAtRisk = registrationDbService.countNbExposedUsersButNotAtRisk();
            final var nbInfectedUsersNotNotified = registrationDbService.countNbUsersAtRiskAndNotNotified();
            final var nbNotifiedUsersScoredAgain = registrationDbService.countNbNotifiedUsersScoredAgain();

            final var nowAtStartOfDay = LocalDate.now().minusDays(1).atStartOfDay().atZone(UTC).toInstant();
            final var todayStatistics = webserviceStatisticsRepository
                    .findByDate(nowAtStartOfDay)
                    .orElse(emptyWebserviceStatistic());
            final var updatedStatistics = todayStatistics.toBuilder()
                    .date(nowAtStartOfDay)
                    // override statistics relative to the service start time
                    .totalAlertedUsers(nbAlertedUsers)
                    .totalExposedButNotAtRiskUsers(nbExposedUsersNotAtRisk)
                    .totalInfectedUsersNotNotified(nbInfectedUsersNotNotified)
                    .totalNotifiedUsersScoredAgain(nbNotifiedUsersScoredAgain)
                    // keep statistics incremented on the fly
                    .notifiedUsers(todayStatistics.getNotifiedUsers())
                    .build();

            webserviceStatisticsRepository.save(updatedStatistics);
            lastKpiComputationInstant = now();
            log.info("KPIs computation successful");
        } catch (Exception e) {
            log.error("unable to compute KPIs", e);
        }
    }

    private static WebserviceStatistics emptyWebserviceStatistic() {
        return new WebserviceStatistics(null, null, null, null, null, null, 0L, 0L);
    }

    private long aggregateUsersAboveThresholdButRetentionPeriodExpired(final List<BatchStatistics> stats) {
        return stats.stream()
                .mapToLong(BatchStatistics::getUsersAboveRiskThresholdButRetentionPeriodExpired)
                .sum();
    }

    private static RobertServerKpiV1 emptyRobertServerKpi() {
        return new RobertServerKpiV1(null, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    public List<RobertServerKpiV1> getKpis(final LocalDate fromDate, final LocalDate toDate) {

        final var range = Range
                .from(inclusive(fromDate.atStartOfDay().toInstant(UTC)))
                .to(exclusive(toDate.plusDays(1).atStartOfDay().toInstant(UTC)));

        final var wsStatsByDate = webserviceStatisticsRepository.getWebserviceStatisticsByDateBetween(range)
                .stream()
                .collect(toMap(stat -> stat.getDate().atZone(UTC).toLocalDate(), Function.identity()));

        final var batchStatsByDate = batchStatisticsRepository.findByJobStartInstantBetween(range)
                .stream()
                .collect(groupingBy(stats -> stats.getJobStartInstant().atZone(UTC).toLocalDate()))
                .entrySet()
                .stream()
                .collect(
                        toMap(
                                Entry::getKey, e -> RobertServerKpiV1.builder()
                                        .date(e.getKey())
                                        .usersAboveRiskThresholdButRetentionPeriodExpired(
                                                aggregateUsersAboveThresholdButRetentionPeriodExpired(e.getValue())
                                        )
                                        .build()
                        )
                );

        return fromDate.datesUntil(toDate.plusDays(1))
                .map(date -> {
                    final var wsStat = wsStatsByDate.getOrDefault(date, emptyWebserviceStatistic());
                    final var batchStat = batchStatsByDate.getOrDefault(date, emptyRobertServerKpi());
                    return RobertServerKpiV1.builder()
                            .date(date)
                            .nbAlertedUsers(wsStat.getTotalAlertedUsers())
                            .nbExposedButNotAtRiskUsers(wsStat.getTotalExposedButNotAtRiskUsers())
                            .nbInfectedUsersNotNotified(wsStat.getTotalInfectedUsersNotNotified())
                            .nbNotifiedUsersScoredAgain(wsStat.getTotalNotifiedUsersScoredAgain())
                            .notifiedUsers(wsStat.getNotifiedUsers())
                            .usersAboveRiskThresholdButRetentionPeriodExpired(
                                    batchStat.getUsersAboveRiskThresholdButRetentionPeriodExpired()
                            )
                            .reportsCount(wsStat.getReportsCount())
                            .build();
                })
                .collect(toList());

    }

    public RobertServerKpiV2 getKpis() {

        final var range = Range
                .from(inclusive(LocalDate.now().atStartOfDay().toInstant(UTC)))
                .to(exclusive(Instant.now()));
        final var wsStats = webserviceStatisticsRepository.findByDate(Instant.now().truncatedTo(DAYS));
        final var batchStats = batchStatisticsRepository.findByJobStartInstantBetween(range);

        final var totalUsersAboveRiskThresholdButRetentionPeriodExpired = batchStats.stream()
                .mapToLong(BatchStatistics::getUsersAboveRiskThresholdButRetentionPeriodExpired).sum();

        final var wsStat = wsStats.orElseGet(KpiService::emptyWebserviceStatistic);
        return RobertServerKpiV2.builder()
                .date(LocalDate.now())
                .nbAlertedUsers(wsStat.getTotalAlertedUsers())
                .nbExposedButNotAtRiskUsers(wsStat.getTotalExposedButNotAtRiskUsers())
                .nbInfectedUsersNotNotified(wsStat.getTotalInfectedUsersNotNotified())
                .nbNotifiedUsersScoredAgain(wsStat.getTotalNotifiedUsersScoredAgain())
                .notifiedUsers(wsStat.getNotifiedUsers())
                .usersAboveRiskThresholdButRetentionPeriodExpired(totalUsersAboveRiskThresholdButRetentionPeriodExpired)
                .reportsCount(wsStat.getReportsCount())
                .build();
    }

    public void incrementNotifiedUsersCount(final Registration registration) {
        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            createWebserviceStatisticIfNotExist();
            webserviceStatisticsRepository.incrementNotifiedUsers(now().truncatedTo(DAYS));
        }
    }

    public void incrementReportsCount() {
        createWebserviceStatisticIfNotExist();
        webserviceStatisticsRepository.incrementReportsCount(now().truncatedTo(DAYS));
    }

    private void createWebserviceStatisticIfNotExist() {

        final var result = webserviceStatisticsRepository.findByDate(now().truncatedTo(DAYS));
        if (result.isEmpty()) {
            if (webserviceStatisticsRepository.count() == 0) {
                webserviceStatisticsRepository.insert(
                        WebserviceStatistics.builder()
                                .date(now().truncatedTo(DAYS))
                                .notifiedUsers(0L)
                                .reportsCount(0L)
                                .build()
                );
            } else {
                final var notifiedUsersTotal = webserviceStatisticsRepository.getAllNotifiedUsersCount().orElse(0L);
                final var reportsCountTotal = webserviceStatisticsRepository.getAllReportsCount().orElse(0L);

                webserviceStatisticsRepository.save(
                        WebserviceStatistics.builder()
                                .date(now().truncatedTo(DAYS))
                                .notifiedUsers(notifiedUsersTotal)
                                .reportsCount(reportsCountTotal)
                                .build()
                );
            }

        }
    }
}
