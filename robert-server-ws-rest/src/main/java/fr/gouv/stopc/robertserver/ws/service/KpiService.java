package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.model.Kpi;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.KpiRepository;
import fr.gouv.stopc.robertserver.ws.api.v2.model.RobertServerKpiV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiRepository kpiRepository;

    private static List<Kpi> emptyKpis() {
        return List.of(
                Kpi.builder().name("alertedUsers").value(0L).build(),
                Kpi.builder().name("exposedButNotAtRiskUsers").value(0L).build(),
                Kpi.builder().name("infectedUsersNotNotified").value(0L).build(),
                Kpi.builder().name("notifiedUsersScoredAgain").value(0L).build(),
                Kpi.builder().name("notifiedUsers").value(0L).build(),
                Kpi.builder().name("reportsCount").value(0L).build()
        );
    }

    public RobertServerKpiV2 getKpis() {

        final var kpis = kpiRepository.findAll();
        if (kpis.isEmpty()) {
            kpiRepository.saveAll(emptyKpis());
        }

        return RobertServerKpiV2.builder()
                .date(OffsetDateTime.now())
                .alertedUsers(
                        kpis
                                .stream()
                                .filter(kpi -> kpi.getName().equals("alertedUsers"))
                                .findAny()
                                .orElseGet(() -> emptyKpi("alertedUsers"))
                                .getValue()

                )
                .exposedButNotAtRiskUsers(
                        kpis
                                .stream()
                                .filter(kpi -> kpi.getName().equals("exposedButNotAtRiskUsers"))
                                .findAny()
                                .orElseGet(() -> emptyKpi("exposedButNotAtRiskUsers"))
                                .getValue()
                )
                .infectedUsersNotNotified(
                        kpis
                                .stream()
                                .filter(kpi -> kpi.getName().equals("infectedUsersNotNotified"))
                                .findAny()
                                .orElseGet(() -> emptyKpi("infectedUsersNotNotified"))
                                .getValue()
                )
                .notifiedUsersScoredAgain(
                        kpis
                                .stream()
                                .filter(kpi -> kpi.getName().equals("notifiedUsersScoredAgain"))
                                .findAny()
                                .orElseGet(() -> emptyKpi("notifiedUsersScoredAgain"))
                                .getValue()
                )
                .notifiedUsers(
                        kpis
                                .stream()
                                .filter(kpi -> kpi.getName().equals("notifiedUsers"))
                                .findAny()
                                .orElseGet(() -> emptyKpi("notifiedUsers"))
                                .getValue()
                )
                .reportsCount(
                        kpis
                                .stream()
                                .filter(kpi -> kpi.getName().equals("reportsCount"))
                                .findAny()
                                .orElseGet(() -> emptyKpi("reportsCount"))
                                .getValue()
                )
                .usersAboveRiskThresholdButRetentionPeriodExpired(
                        kpis
                                .stream()
                                .filter(kpi -> kpi.getName().equals("usersAboveRiskThresholdButRetentionPeriodExpired"))
                                .findAny()
                                .orElseGet(() -> emptyKpi("usersAboveRiskThresholdButRetentionPeriodExpired"))
                                .getValue()
                )
                .build();
    }

    public void incrementNotifiedUsersCount(final Registration registration) {
        if (!registration.isNotifiedForCurrentRisk() && registration.isAtRisk()) {
            kpiRepository.incrementKpi("notifiedUsers");
        }
    }

    public void incrementReportsCount() {
        kpiRepository.incrementKpi("reportsCount");
    }

    public void incrementAlertedUsers() {
        kpiRepository.incrementKpi("alertedUsers");
    }

    private Kpi emptyKpi(final String name) {
        return Kpi.builder().name(name).value(0L).build();
    }

}
