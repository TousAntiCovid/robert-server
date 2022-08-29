package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.robert.client.api.KpiApi;
import fr.gouv.stopc.robert.client.model.RobertServerKpi;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class RobertKpiSteps {

    private final KpiApi kpiApi;

    private RobertServerKpi robertKpiSnapshot;

    @Before
    public void takeKpisSnapshot() {
        robertKpiSnapshot = kpiApi.kpis();
    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired est incrémenté de {long}")
    public void checkKpiIncrement(final Long increment) {

        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(robertKpiSnapshot.getUsersAboveRiskThresholdButRetentionPeriodExpired() + increment);

    }

    @Alors("le kpi notifiedUsers est incrémenté de {long}")
    public void checkNotifiedUsersIncrement(final Long increment) {

        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getNotifiedUsers())
                .isEqualTo(robertKpiSnapshot.getNotifiedUsers() + increment);
    }

    @Alors("le kpi reportsCount est incrémenté de {long}")
    public void checkReportsCountIncrement(final Long increment) {

        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getReportsCount())
                .isEqualTo(robertKpiSnapshot.getReportsCount() + increment);
    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired n'a pas changé")
    public void compareUsersAboveRiskThresholdButRetentionPeriodExpiredKpiValue() {

        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(robertKpiSnapshot.getUsersAboveRiskThresholdButRetentionPeriodExpired());

    }

    @Alors("le kpi notifiedUsers n'a pas changé")
    public void compareNotifiedUsersKpiValue() {

        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(robertKpiSnapshot.getUsersAboveRiskThresholdButRetentionPeriodExpired());

    }

}
