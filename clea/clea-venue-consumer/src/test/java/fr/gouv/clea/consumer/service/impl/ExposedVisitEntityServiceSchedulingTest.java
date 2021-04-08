package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext
@TestPropertySource(properties = {"clea.conf.scheduling.purge.cron=*/10 * * * * *", "clea.conf.scheduling.purge.enabled=true"})
class ExposedVisitEntityServiceSchedulingTest {

    @Value("${clea.conf.scheduling.purge.cron}")
    private String cronValue;

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    private static ExposedVisitEntity createExposedVisit(int timeSlot, long periodStart) {
        return new ExposedVisitEntity(
                null, // handled by db
                UUID.randomUUID(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                periodStart,
                timeSlot,
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                Instant.now(),
                null, // handled by db
                null // handled by db
        );
    }

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("check that croned job is active")
    void testCronIsActive() {
        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        scheduledTasks.forEach(scheduledTask -> scheduledTask.getTask().getRunnable().getClass().getDeclaredMethods());
        long count = scheduledTasks.stream()
                .filter(scheduledTask -> scheduledTask.getTask() instanceof CronTask)
                .map(scheduledTask -> (CronTask) scheduledTask.getTask())
                .filter(cronTask -> cronTask.getExpression().equals(cronValue)
                        && cronTask.toString().equals("fr.gouv.clea.consumer.service.impl.ExposedVisitEntityService.deleteOutdatedExposedVisits"))
                .count();

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("check that croned job remove outdated exposed visits from DB")
    void deleteOutdatedExposedVisits() {
        Instant now = Instant.now();

        long _15DaysLater = TimeUtils.ntpTimestampFromInstant(now.plus(15, ChronoUnit.DAYS));
        long _14DaysLater = TimeUtils.ntpTimestampFromInstant(now.plus(14, ChronoUnit.DAYS));
        long _2DaysLater = TimeUtils.ntpTimestampFromInstant(now.plus(2, ChronoUnit.DAYS));
        long yesterday = TimeUtils.ntpTimestampFromInstant(now.minus(1, ChronoUnit.DAYS));
        long _2DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(2, ChronoUnit.DAYS));
        long _14DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(14, ChronoUnit.DAYS));
        long _15DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(15, ChronoUnit.DAYS));
        long _16DaysAgo = TimeUtils.ntpTimestampFromInstant(now.minus(16, ChronoUnit.DAYS));

        repository.saveAll(
                List.of(
                        createExposedVisit(1, _15DaysLater), // keep
                        createExposedVisit(2, _14DaysLater), // keep
                        createExposedVisit(3, _2DaysLater), // keep
                        createExposedVisit(4, yesterday), // keep
                        createExposedVisit(5, _2DaysAgo), // keep
                        createExposedVisit(0, _14DaysAgo), // keep, will only be purged if query condition is >=
                        createExposedVisit(1, _14DaysAgo), // keep
                        createExposedVisit(0, _15DaysAgo), // purge
                        createExposedVisit(9, _16DaysAgo) // purge
                )
        );

        assertThat(repository.count()).isEqualTo(9);

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(repository.count()).isEqualTo(7));

        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == _15DaysLater)).isTrue();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == _14DaysLater)).isTrue();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == _2DaysLater)).isTrue();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == yesterday)).isTrue();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == _2DaysAgo)).isTrue();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == _14DaysAgo)).isTrue();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == _15DaysAgo)).isFalse();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getPeriodStart() == _16DaysAgo)).isFalse();
    }
}