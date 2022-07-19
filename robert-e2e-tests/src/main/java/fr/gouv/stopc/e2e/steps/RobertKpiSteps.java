package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.robert.client.api.KpiApi;
import fr.gouv.stopc.robert.client.model.RobertServerKpiV1;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class RobertKpiSteps {

    private final PlatformTimeSteps platformTimeSteps;

    private final KpiApi kpiApi;

    private final List<RobertServerKpiV1> robertServerKpiHistory;

    @Before
    public void takeKpisSnapshot() {
        robertServerKpiHistory.clear();
        final var robertServerKpis = kpiApi.kpi(LocalDate.now().minus(30, ChronoUnit.DAYS), LocalDate.now());
        robertServerKpiHistory.addAll(robertServerKpis);
    }

    @Alors("le kpi {word} est {long}")
    public void checkKpiValue(final String kpiName, final Long value) {

        final var robertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformDate());
        assertThat(robertServerKpi).hasFieldOrPropertyWithValue(kpiName, value);

    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired est incrémenté de {long}")
    public void checkKpiIncrement(final Long increment) {

        final var oldRobertServerKpi = getCurrentKpiFromSnapshot();
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformDate());

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired() + increment);

    }

    @Alors("le kpi notifiedUsers est incrémenté de {long}")
    public void checkNotifiedUsersIncrement(final Long increment) {

        final var oldRobertServerKpi = getCurrentKpiFromSnapshot();
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformDate());

        assertThat(todayRobertServerKpi.getNotifiedUsers())
                .isEqualTo(oldRobertServerKpi.getNotifiedUsers() + increment);
    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired n'a pas changé")
    public void compareUsersAboveRiskThresholdButRetentionPeriodExpiredKpiValue() {

        final var oldRobertServerKpi = getKpiFromHistory(platformTimeSteps.getPlatformDate());
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformDate());

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired());

    }

    @Alors("le kpi notifiedUsers n'a pas changé")
    public void compareNotifiedUsersKpiValue() {

        final var oldRobertServerKpi = getKpiFromHistory(platformTimeSteps.getPlatformDate());
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformDate());

        assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired());

    }

    public RobertServerKpiV1 getRobertServerKpiAt(LocalDate localDate) {

        final var kpis = kpiApi.kpi(localDate, localDate);
        assertThat(kpis.size()).isEqualTo(1);
        return kpis.get(0);

    }

    private RobertServerKpiV1 getCurrentKpiFromSnapshot() {
        return robertServerKpiHistory.stream()
                .filter(
                        kpi -> kpi.getDate().equals(
                                LocalDate.ofInstant(platformTimeSteps.getPlatformTime(), UTC)
                        )
                )
                .findAny()
                .get();
    }

    private RobertServerKpiV1 getKpiFromHistory(LocalDate localDate) {
        return robertServerKpiHistory.stream()
                .filter(kpi -> kpi.getDate().equals(localDate))
                .findAny()
                .get();
    }

}
