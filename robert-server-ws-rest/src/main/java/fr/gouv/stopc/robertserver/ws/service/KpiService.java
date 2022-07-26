package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.model.BatchStatistics;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.WebserviceKpi;
import fr.gouv.stopc.robertserver.database.repository.BatchStatisticsRepository;
import fr.gouv.stopc.robertserver.database.repository.WebserviceKpiRepository;
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

    private final WebserviceKpiRepository webserviceKpiRepository;

    private final BatchStatisticsRepository batchStatisticsRepository;

    public KpiService(
            final WebserviceKpiRepository webserviceKpiRepository,
            final BatchStatisticsRepository batchStatisticsRepository) {
        this.webserviceKpiRepository = webserviceKpiRepository;
        this.batchStatisticsRepository = batchStatisticsRepository;
    }

    private static List<WebserviceKpi> emptyWebserviceKpis() {
        return List.of(
                WebserviceKpi.builder().name("alertedUsers").value(0L).build(),
                WebserviceKpi.builder().name("exposedButNotAtRiskUsers").value(0L).build(),
                WebserviceKpi.builder().name("infectedUsersNotNotified").value(0L).build(),
                WebserviceKpi.builder().name("notifiedUsersScoredAgain").value(0L).build(),
                WebserviceKpi.builder().name("notifiedUsers").value(0L).build(),
                WebserviceKpi.builder().name("reportsCount").value(0L).build()
        );
    }

    public RobertServerKpiV2 getKpis() {

        final var range = Range
                .from(inclusive(LocalDate.now().atStartOfDay().toInstant(UTC)))
                .to(exclusive(Instant.now()));
        final var webserviceKpis = webserviceKpiRepository.findAll();
        if (webserviceKpis.isEmpty()) {
            webserviceKpiRepository.saveAll(emptyWebserviceKpis());
        }

        final var batchStats = batchStatisticsRepository.findByJobStartInstantBetween(range);

        final var totalUsersAboveRiskThresholdButRetentionPeriodExpired = batchStats.stream()
                .mapToLong(BatchStatistics::getUsersAboveRiskThresholdButRetentionPeriodExpired).sum();

        return RobertServerKpiV2.builder()
                .date(LocalDate.now())
                .alertedUsers(
                        webserviceKpis.stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("alertedUsers"))
                                .findAny()
                                .orElseGet(() -> emptyWebserviceKpi("alertedUsers"))
                                .getValue()

                )
                .exposedButNotAtRiskUsers(
                        webserviceKpis.stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("exposedButNotAtRiskUsers"))
                                .findAny()
                                .orElseGet(() -> emptyWebserviceKpi("exposedButNotAtRiskUsers"))
                                .getValue()
                )
                .infectedUsersNotNotified(
                        webserviceKpis.stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("infectedUsersNotNotified"))
                                .findAny()
                                .orElseGet(() -> emptyWebserviceKpi("infectedUsersNotNotified"))
                                .getValue()
                )
                .notifiedUsersScoredAgain(
                        webserviceKpis.stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("notifiedUsersScoredAgain"))
                                .findAny()
                                .orElseGet(() -> emptyWebserviceKpi("notifiedUsersScoredAgain"))
                                .getValue()
                )
                .notifiedUsers(
                        webserviceKpis.stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("notifiedUsers"))
                                .findAny()
                                .orElseGet(() -> emptyWebserviceKpi("notifiedUsers"))
                                .getValue()
                )
                .reportsCount(
                        webserviceKpis.stream()
                                .filter(webserviceKpi -> webserviceKpi.getName().equals("reportsCount"))
                                .findAny()
                                .orElseGet(() -> emptyWebserviceKpi("reportsCount"))
                                .getValue()
                )
                .usersAboveRiskThresholdButRetentionPeriodExpired(totalUsersAboveRiskThresholdButRetentionPeriodExpired)
                .build();
    }

    public void incrementNotifiedUsersCount(final Registration registration) {
        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            webserviceKpiRepository.incrementNotifiedUsers();
        }
    }

    public void incrementReportsCount() {
        webserviceKpiRepository.incrementReportsCount();
    }

    private WebserviceKpi emptyWebserviceKpi(final String name) {
        return WebserviceKpi.builder().name(name).value(0L).build();
    }

}
