package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.data.ExposedVisit;
import fr.gouv.clea.consumer.data.IExposedVisitRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.lang.math.RandomUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(
        properties = {
                "clea.conf.scheduling.purge.cron=*/10 * * * * *",
                "clea.conf.scheduling.purge.enabled=true"
        }
)
class PersistServiceSchedulingTest {

    @Value("${clea.conf.scheduling.purge.cron}")
    private String cronValue;

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    private static ExposedVisit createExposedVisit(Instant qrCodeScanTime) {
        return new ExposedVisit(
                null, // handled by db
                UUID.randomUUID().toString(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                qrCodeScanTime,
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
                        && cronTask.toString().equals("fr.gouv.clea.consumer.service.impl.PersistService.deleteOutdatedExposedVisits"))
                .count();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("check that croned job remove outdated exposed visits from DB")
    void deleteOutdatedExposedVisits() {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant _14DaysAgo = now.minus(14, ChronoUnit.DAYS);
        Instant _15DaysAgo = now.minus(15, ChronoUnit.DAYS);
        repository.saveAll(
                List.of(
                        createExposedVisit(yesterday), // keep
                        createExposedVisit(_14DaysAgo), // remove, will always be < now
                        createExposedVisit(_15DaysAgo) // remove
                )
        );
        assertThat(repository.count()).isEqualTo(3);
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(repository.count()).isEqualTo(1));
        assertThat(repository.findAll().stream().anyMatch(it -> it.getQrCodeScanTime().equals(yesterday))).isTrue();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getQrCodeScanTime().equals(_14DaysAgo))).isFalse();
        assertThat(repository.findAll().stream().anyMatch(it -> it.getQrCodeScanTime().equals(_15DaysAgo))).isFalse();
    }
}