package fr.gouv.stopc.robertserver.ws.service;

import fr.gouv.stopc.robertserver.database.model.Kpi;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.KpiRepository;
import fr.gouv.stopc.robertserver.ws.api.model.RobertServerKpi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiRepository kpiRepository;

    public RobertServerKpi getKpis() {

        final var kpis = kpiRepository.findAll();
        return RobertServerKpi.builder()
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
