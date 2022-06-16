package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import fr.gouv.stopc.robertserver.ws.api.model.RobertServerKpi;
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

    private Instant lastKpiComputationInstant = Instant.ofEpochSecond(0);

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

    public List<RobertServerKpi> getKpis(final LocalDate fromDate, final LocalDate toDate) {

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
                                Entry::getKey, e -> RobertServerKpi.builder()
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
                    return RobertServerKpi.builder()
                            .date(date)
                            .nbAlertedUsers(wsStat.getTotalAlertedUsers())
                            .nbExposedButNotAtRiskUsers(wsStat.getTotalExposedButNotAtRiskUsers())
                            .nbInfectedUsersNotNotified(wsStat.getTotalInfectedUsersNotNotified())
                            .nbNotifiedUsersScoredAgain(wsStat.getTotalNotifiedUsersScoredAgain())
                            .notifiedUsers(wsStat.getNotifiedUsers())
                            .usersAboveRiskThresholdButRetentionPeriodExpired(
                                    batchStat.getUsersAboveRiskThresholdButRetentionPeriodExpired()
                            )
                            .build();
                })
                .collect(toList());

    }

    private long aggregateUsersAboveThresholdButRetentionPeriodExpired(final List<BatchStatistics> stats) {
        return stats.stream()
                .mapToLong(BatchStatistics::getUsersAboveRiskThresholdButRetentionPeriodExpired)
                .sum();
    }

    public void updateWebserviceStatistics(final Registration registration) {
        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            webserviceStatisticsRepository.incrementNotifiedUsers(now().truncatedTo(DAYS));
        }
    }

    private static WebserviceStatistics emptyWebserviceStatistic() {
        return new WebserviceStatistics(null, null, null, null, null, null, 0L);
    }

    private static RobertServerKpi emptyRobertServerKpi() {
        return new RobertServerKpi(null, 0L, 0L, 0L, 0L, 0L, 0L);
    }
}
