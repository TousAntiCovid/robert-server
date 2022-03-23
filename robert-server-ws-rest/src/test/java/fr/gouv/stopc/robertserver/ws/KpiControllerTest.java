package fr.gouv.stopc.robertserver.ws;

import fr.gouv.stopc.robertserver.database.model.Statistic;
import fr.gouv.stopc.robertserver.database.repository.StatisticsRepository;
import fr.gouv.stopc.robertserver.ws.dto.RobertServerKpi;
import fr.gouv.stopc.robertserver.ws.service.IKpiService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;

class KpiControllerTest {

    @Autowired
    private IKpiService kpiService;

    @Autowired
    private StatisticsRepository repository;

    @BeforeEach
    void beforeEach() {
        repository.deleteAll();
    }

    private static Statistic defaultStatistic() {
        return Statistic.builder()
                .date(Instant.now())
                .notifiedTotal(0)
                .build();
    }

    @Test
    void should_retrieve_a_statistic_with_just_1_notification() {

        final var statistic = defaultStatistic().toBuilder()
                .notifiedTotal(1)
                .build();

        repository.save(statistic);

        final var robertServerKpis = kpiService.computeKpi(
                LocalDate.now(ZoneId.systemDefault()).minus(1, DAYS),
                LocalDate.now(ZoneId.systemDefault()).plus(1, DAYS)
        );

        Assertions.assertThat(robertServerKpis).containsExactly(
                new RobertServerKpi(
                        LocalDate.now(),
                        0L,
                        0L,
                        0L,
                        0L,
                        1
                )
        );

    }

    @Test
    void should_not_retrieve_a_statistic() {

        final var statistic = defaultStatistic().toBuilder()
                .date(Instant.now().minus(2, DAYS))
                .notifiedTotal(1)
                .build();

        repository.save(statistic);

        final var robertServerKpis = kpiService.computeKpi(
                LocalDate.now(ZoneId.systemDefault()).minus(1, DAYS),
                LocalDate.now(ZoneId.systemDefault()).plus(1, DAYS)
        );

        Assertions.assertThat(robertServerKpis).isEmpty();

    }

    @Test
    void should_aggregate_yesterday_and_today_statistic_with_3_total_notifications() {

        final var oneStatistic = defaultStatistic().toBuilder()
                .date(Instant.now().minus(1, DAYS))
                .notifiedTotal(1)
                .build();

        final var anOtherStatistic = defaultStatistic().toBuilder()
                .date(Instant.now())
                .notifiedTotal(2)
                .build();

        final var aThirdStatistic = defaultStatistic().toBuilder()
                .date(Instant.now().plus(1, DAYS))
                .notifiedTotal(5)
                .build();

        repository.saveAll(
                List.of(
                        oneStatistic,
                        anOtherStatistic,
                        aThirdStatistic
                )
        );

        final var robertServerKpis = kpiService.computeKpi(
                LocalDate.now(ZoneId.systemDefault()).minus(1, DAYS),
                LocalDate.now(ZoneId.systemDefault()).plus(1, DAYS)
        );

        Assertions.assertThat(robertServerKpis).containsExactly(
                new RobertServerKpi(
                        LocalDate.now(),
                        0L,
                        0L,
                        0L,
                        0L,
                        3
                )
        );

    }

}
