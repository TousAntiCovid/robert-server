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
import org.springframework.data.domain.Range;
import org.springframework.data.util.StreamUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.Range.Bound.exclusive;
import static org.springframework.data.domain.Range.Bound.inclusive;

@Service
public class KpiService {

    private final IRegistrationService registrationDbService;

    private final WebserviceStatisticsRepository webserviceStatisticsRepository;

    private final BatchStatisticsRepository batchStatisticsRepository;

    private Instant lastKpiComputationTimeInSeconds = Instant.ofEpochSecond(0);

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
                        "robert.dailystatistics.lastsuccess.age",
                        () -> now().getEpochSecond() - lastKpiComputationTimeInSeconds.getEpochSecond()
                )
                .description("last kpi computation success")
                .baseUnit("seconds")
                .register(meterRegistry);
    }

    public void computeDailyKpis() {
        final var nbAlertedUsers = registrationDbService.countNbUsersNotified();
        final var nbExposedUsersNotAtRisk = registrationDbService.countNbExposedUsersButNotAtRisk();
        final var nbInfectedUsersNotNotified = registrationDbService.countNbUsersAtRiskAndNotNotified();
        final var nbNotifiedUsersScoredAgain = registrationDbService.countNbNotifiedUsersScoredAgain();

        final var nowAtStartOfDay = LocalDate.now().minusDays(1).atStartOfDay().atZone(UTC).toInstant();
        final var todayStatistics = webserviceStatisticsRepository
                .findByDate(nowAtStartOfDay)
                .orElse(new WebserviceStatistics(null, nowAtStartOfDay, 0L, 0L, 0L, 0L, 0L));
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
        lastKpiComputationTimeInSeconds = now();
    }

    public List<RobertServerKpi> getKpis(final LocalDate fromDate, final LocalDate toDate) {

        final var range = Range
                .from(inclusive(fromDate.atStartOfDay().toInstant(UTC)))
                .to(exclusive(toDate.plusDays(1).atStartOfDay().toInstant(UTC)));

        final var wsStats = webserviceStatisticsRepository
                .getWebserviceStatisticsByDateBetween(range)
                .stream()
                .sorted(comparing(WebserviceStatistics::getDate));
        final var batchStats = batchStatisticsRepository.findByJobStartInstantBetween(range)
                .stream()
                .collect(groupingBy(stats -> stats.getJobStartInstant().atZone(UTC).toLocalDate()))
                .entrySet()
                .stream()
                .map(
                        e -> RobertServerKpi.builder()
                                .date(e.getKey())
                                .usersAboveRiskThresholdButRetentionPeriodExpired(
                                        e.getValue()
                                                .stream()
                                                .mapToLong(
                                                        BatchStatistics::getUsersAboveRiskThresholdButRetentionPeriodExpired
                                                )
                                                .sum()
                                )
                                .build()
                )
                .sorted(comparing(RobertServerKpi::getDate));
        return StreamUtils.zip(
                wsStats, batchStats, (wsStat, batchStat) -> RobertServerKpi.builder()
                        .date(LocalDate.ofInstant(wsStat.getDate(), UTC))
                        .nbAlertedUsers(wsStat.getTotalAlertedUsers())
                        .nbExposedButNotAtRiskUsers(wsStat.getTotalExposedButNotAtRiskUsers())
                        .nbInfectedUsersNotNotified(wsStat.getTotalInfectedUsersNotNotified())
                        .nbNotifiedUsersScoredAgain(wsStat.getTotalNotifiedUsersScoredAgain())
                        .nbNotifiedUsers(wsStat.getNotifiedUsers())
                        .usersAboveRiskThresholdButRetentionPeriodExpired(
                                batchStat.getUsersAboveRiskThresholdButRetentionPeriodExpired()
                        )
                        .build()
        ).collect(toList());

    }

    public void updateWebserviceStatistics(final Registration registration) {
        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            webserviceStatisticsRepository.incrementNotifiedUsers(now().truncatedTo(DAYS));
        }
    }
}
