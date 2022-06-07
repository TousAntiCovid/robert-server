package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.robert.client.api.KpiApi;
import fr.gouv.stopc.robert.client.model.RobertServerKpi;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.Instant.*;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class RobertKpiSteps {

    private final PlatformTimeSteps platformTimeSteps;

    private final KpiApi kpiApi;

    private final List<RobertServerKpi> robertServerKpiHistory;

    @Before
    public void takeKpisSnapshot() {
        takeKpisSnapshot(now().minus(30, ChronoUnit.DAYS), now());
    }

    @Alors("le kpi {word} est {long}")
    public void checkKpiValue(final String kpiName, final Long value) {

        final var robertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());
        assertThat(robertServerKpi).hasFieldOrPropertyWithValue(kpiName, value);

    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired est incrémenté de {long}")
    public void checkKpiIncrement(final Long increment) {

        final var oldRobertServerKpi = getCurrentKpiFromSnapshot();
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());

        if (todayRobertServerKpi == null) {
            throw new RuntimeException("No KPI to compare");
        } else {
            assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                    .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired() + increment);
        }
    }

    @Alors("le kpi notifiedUsers est incrémenté de {long}")
    public void checkNotifiedUsersIncrement(final Long increment) {

        final var oldRobertServerKpi = getCurrentKpiFromSnapshot();
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());

        if (todayRobertServerKpi == null) {
            throw new RuntimeException("No KPI to compare");
        } else {
            assertThat(todayRobertServerKpi.getNotifiedUsers())
                    .isEqualTo(oldRobertServerKpi.getNotifiedUsers() + increment);
        }
    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired n'a pas changé")
    public void compareUsersAboveRiskThresholdButRetentionPeriodExpiredKpiValue() {

        final var oldRobertServerKpi = getKpiFromHistory(platformTimeSteps.getPlatformTime());
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());

        if (todayRobertServerKpi == null) {
            throw new RuntimeException("No KPI to compare");
        } else {
            assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                    .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired());
        }

    }

    @Alors("le kpi notifiedUsers n'a pas changé")
    public void compareNotifiedUsersKpiValue() {

        final var oldRobertServerKpi = getKpiFromHistory(platformTimeSteps.getPlatformTime());
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());

        if (todayRobertServerKpi == null) {
            throw new RuntimeException("No KPI to compare");
        } else {
            assertThat(todayRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                    .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired());
        }

    }

    public RobertServerKpi getRobertServerKpiAt(Instant instant) {

        final var kpis = kpiApi.kpi(
                LocalDate.ofInstant(instant, UTC),
                LocalDate.ofInstant(instant, UTC)
        );
        if (kpis.size() != 1) {
            throw new RuntimeException("expecting one kpi but got " + kpis.size());
        }
        return kpis.get(0);

    }

    private RobertServerKpi getCurrentKpiFromSnapshot() {
        return robertServerKpiHistory.stream()
                .filter(
                        kpi -> kpi.getDate().equals(
                                LocalDate.ofInstant(platformTimeSteps.getPlatformTime(), UTC)
                        )
                )
                .findFirst()
                .orElse(
                        RobertServerKpi.builder()
                                .date(LocalDate.ofInstant(platformTimeSteps.getPlatformTime(), UTC))
                                .nbAlertedUsers(null)
                                .nbExposedButNotAtRiskUsers(null)
                                .nbInfectedUsersNotNotified(null)
                                .nbNotifiedUsersScoredAgain(null)
                                .notifiedUsers(0L)
                                .usersAboveRiskThresholdButRetentionPeriodExpired(0L)
                                .build()
                );
    }

    private RobertServerKpi getKpiFromHistory(Instant instant) {
        return robertServerKpiHistory.stream()
                .filter(kpi -> kpi.getDate().equals(LocalDate.ofInstant(instant, UTC)))
                .findFirst()
                .orElseThrow(() -> {
                    throw new RuntimeException("No kpi found in history, probably an early call or bad initialization");
                });
    }

    private void takeKpisSnapshot(
            final Instant from,
            final Instant to) {

        robertServerKpiHistory.clear();
        final var robertServerKpis = kpiApi.kpi(
                LocalDate.ofInstant(from, UTC),
                LocalDate.ofInstant(to, UTC)
        );
        robertServerKpiHistory.addAll(robertServerKpis);
    }

}
