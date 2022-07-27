package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.robert.client.api.KpiApi;
import fr.gouv.stopc.robert.client.model.RobertServerKpiV2;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class RobertKpiSteps {

    private final PlatformTimeSteps platformTimeSteps;

    private final KpiApi kpiApi;

    private final List<RobertServerKpiV2> robertServerKpiHistory;

    @Before
    public void takeKpisSnapshot() {
        robertServerKpiHistory.clear();
        final var robertServerKpi = kpiApi.kpis();
        robertServerKpiHistory.add(robertServerKpi);
    }

    @Alors("le kpi {word} est {long}")
    public void checkKpiValue(final String kpiName, final Long value) {

        final var robertServerKpi = kpiApi.kpis();
        assertThat(robertServerKpi).hasFieldOrPropertyWithValue(kpiName, value);

    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired est incrémenté de {long}")
    public void checkKpiIncrement(final Long increment) {

        final var oldRobertServerKpi = getCurrentKpiFromSnapshot();
        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired() + increment);

    }

    @Alors("le kpi notifiedUsers est incrémenté de {long}")
    public void checkNotifiedUsersIncrement(final Long increment) {

        final var oldRobertServerKpi = getCurrentKpiFromSnapshot();
        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getNotifiedUsers())
                .isEqualTo(oldRobertServerKpi.getNotifiedUsers() + increment);
    }

    @Alors("le kpi reportsCount est incrémenté de {long}")
    public void checkReportsCountIncrement(final Long increment) {

        final var oldRobertServerKpi = getCurrentKpiFromSnapshot();
        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getReportsCount())
                .isEqualTo(oldRobertServerKpi.getReportsCount() + increment);
    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired n'a pas changé")
    public void compareUsersAboveRiskThresholdButRetentionPeriodExpiredKpiValue() {

        final var oldRobertServerKpi = getKpiFromHistory(platformTimeSteps.getPlatformDate());
        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired());

    }

    @Alors("le kpi notifiedUsers n'a pas changé")
    public void compareNotifiedUsersKpiValue() {

        final var oldRobertServerKpi = getKpiFromHistory(platformTimeSteps.getPlatformDate());
        final var todayRobertServerKpi = kpiApi.kpis();

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired());

    }

    private RobertServerKpiV2 getCurrentKpiFromSnapshot() {
        return robertServerKpiHistory.stream()
                .filter(kpi -> kpi.getDate().toLocalDate().equals(platformTimeSteps.getPlatformDate()))
                .findAny()
                .orElseThrow();
    }

    private RobertServerKpiV2 getKpiFromHistory(LocalDate localDate) {
        return robertServerKpiHistory.stream()
                .filter(kpi -> kpi.getDate().equals(localDate))
                .findAny()
                .orElseThrow();
    }

}
