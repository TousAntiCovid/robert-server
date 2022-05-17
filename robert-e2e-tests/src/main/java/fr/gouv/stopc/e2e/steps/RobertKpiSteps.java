package fr.gouv.stopc.e2e.steps;

import fr.gouv.stopc.e2e.kpis.repository.BatchStatisticsRepository;
import fr.gouv.stopc.e2e.kpis.repository.WebserviceStatisticsRepository;
import fr.gouv.stopc.robert.client.api.KpiApi;
import fr.gouv.stopc.robert.client.model.RobertServerKpi;
import io.cucumber.java.Before;
import io.cucumber.java.fr.Alors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class RobertKpiSteps {

    private final PlatformTimeSteps platformTimeSteps;

    private final Map<LocalDate, RobertServerKpi> robertServerKpiHistory;

    private final BatchStatisticsRepository batchStatisticsRepository;

    private final WebserviceStatisticsRepository webserviceStatisticsRepository;

    @Autowired
    private final KpiApi kpiApi;

    @Before
    public void deleteAllExistingKpis() {
        batchStatisticsRepository.deleteAll();
        webserviceStatisticsRepository.deleteAll();

    }

    @Alors("le kpi {word} est {long}")
    public void checkKpiValue(final String kpiName,
            final Long value) {

        final var robertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());
        robertServerKpi.ifPresentOrElse(kpi -> {
            assertThat(kpi).hasFieldOrPropertyWithValue(kpiName, value);
            robertServerKpiHistory.put(LocalDate.ofInstant(platformTimeSteps.getPlatformTime(), UTC), kpi);
        }, () -> {
            throw new RuntimeException("No KPI was found for " + kpiName);
        });
    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired est incrémenté de {long} par rapport à il y a {duration}")
    public void checkKpiValue(final Long increment,
            final Duration durationAgo) {

        final var oldRobertServerKpi = robertServerKpiHistory
                .get(LocalDate.ofInstant(platformTimeSteps.getPlatformTime().minus(durationAgo), UTC));
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());
        todayRobertServerKpi.ifPresent(
                kpi -> assertThat(kpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                        .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired() + increment)
        );

    }

    @Alors("le kpi usersAboveRiskThresholdButRetentionPeriodExpired n'a pas changé par rapport à il y a {duration}")
    public void checkKpiValue(final Duration durationAgo) {

        final var oldRobertServerKpi = robertServerKpiHistory
                .get(LocalDate.ofInstant(platformTimeSteps.getPlatformTime().minus(durationAgo), UTC));
        final var todayRobertServerKpi = getRobertServerKpiAt(platformTimeSteps.getPlatformTime());
        todayRobertServerKpi.ifPresent(
                kpi -> assertThat(kpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
                        .isEqualTo(oldRobertServerKpi.getUsersAboveRiskThresholdButRetentionPeriodExpired())
        );
    }

    public Optional<RobertServerKpi> getRobertServerKpiAt(Instant instant) {
        final var robertServerKpi = kpiApi.kpi(
                LocalDate.ofInstant(instant, UTC),
                LocalDate.ofInstant(instant, UTC)
        )
                .stream()
                .findFirst();

        robertServerKpi.ifPresent(this::addRobertServerKpiInHistory);
        return robertServerKpi;
    }

    public void addRobertServerKpiInHistory(RobertServerKpi robertServerKpiToSave) {
        robertServerKpiHistory.put(robertServerKpiToSave.getDate(), robertServerKpiToSave);
    }

}
