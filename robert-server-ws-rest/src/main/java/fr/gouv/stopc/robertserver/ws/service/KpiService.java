package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceKpi;
import fr.gouv.stopc.robertserver.database.model.WebserviceStatistics;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.WebserviceStatisticsRepository;
import fr.gouv.stopc.robertserver.ws.api.v2.model.RobertServerKpiV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.springframework.data.domain.Range.Bound.exclusive;
import static org.springframework.data.domain.Range.Bound.inclusive;

@Slf4j
@Service
public class KpiService {

    private final WebserviceStatisticsRepository webserviceStatisticsRepository;

    private final BatchStatisticsRepository batchStatisticsRepository;

    public KpiService(
            final WebserviceStatisticsRepository webserviceStatisticsRepository,
            final BatchStatisticsRepository batchStatisticsRepository) {
        this.webserviceStatisticsRepository = webserviceStatisticsRepository;
        this.batchStatisticsRepository = batchStatisticsRepository;
    }

    private static WebserviceStatistics emptyWebserviceStatistic() {
        return new WebserviceStatistics(
                null, List.of(
                        WebserviceKpi.builder().name("alertedUsers").value(0L).build(),
                        WebserviceKpi.builder().name("exposedButNotAtRiskUsers").value(0L).build(),
                        WebserviceKpi.builder().name("infectedUsersNotNotified").value(0L).build(),
                        WebserviceKpi.builder().name("notifiedUsersScoredAgain").value(0L).build(),
                        WebserviceKpi.builder().name("notifiedUsers").value(0L).build(),
                        WebserviceKpi.builder().name("reportsCount").value(0L).build()
                )
        );
    }

    public RobertServerKpiV2 getKpis() {

        final var range = Range
                .from(inclusive(LocalDate.now().atStartOfDay().toInstant(UTC)))
                .to(exclusive(Instant.now()));
        final var webserviceStatistics = webserviceStatisticsRepository.findAll();
        final WebserviceStatistics webserviceStats;
        if (webserviceStatistics.isEmpty()) {
            webserviceStats = emptyWebserviceStatistic();
        } else {
            webserviceStats = webserviceStatistics.get(0);
        }
        final var batchStats = batchStatisticsRepository.findByJobStartInstantBetween(range);

        final var totalUsersAboveRiskThresholdButRetentionPeriodExpired = batchStats.stream()
                .mapToLong(BatchStatistics::getUsersAboveRiskThresholdButRetentionPeriodExpired).sum();

        return RobertServerKpiV2.builder()
                .date(LocalDate.now())
                .alertedUsers(
                        webserviceStats.getStatistics().stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("alertedUsers"))
                                .findFirst()
                                .get()
                                .getValue()
                )
                .exposedButNotAtRiskUsers(
                        webserviceStats.getStatistics().stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("exposedButNotAtRiskUsers"))
                                .findFirst()
                                .get()
                                .getValue()
                )
                .infectedUsersNotNotified(
                        webserviceStats.getStatistics().stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("infectedUsersNotNotified"))
                                .findFirst()
                                .get()
                                .getValue()
                )
                .notifiedUsersScoredAgain(
                        webserviceStats.getStatistics().stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("notifiedUsersScoredAgain"))
                                .findFirst()
                                .get()
                                .getValue()
                )
                .notifiedUsers(
                        webserviceStats.getStatistics().stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("notifiedUsers"))
                                .findFirst()
                                .get()
                                .getValue()
                )
                .usersAboveRiskThresholdButRetentionPeriodExpired(totalUsersAboveRiskThresholdButRetentionPeriodExpired)
                .reportsCount(
                        webserviceStats.getStatistics().stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("reportsCount"))
                                .findFirst()
                                .get()
                                .getValue()
                )
                .build();
    }

    public void incrementNotifiedUsersCount(final Registration registration) {
        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            webserviceStatisticsRepository.incrementNotifiedUsers();
        }
    }

    public void incrementReportsCount() {
        webserviceStatisticsRepository.incrementReportsCount();
    }

}
