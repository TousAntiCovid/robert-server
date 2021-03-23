package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.data.DetectedVenue;
import fr.gouv.tacw.data.IDetectedVenueRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(
        properties = {
                "clea.conf.scheduling.purge.cron=*/5 * * * * *",
                "clea.conf.scheduling.purge.enabled=true"
        }
)
class PersistServiceSchedulingTest {

    @Autowired
    private IDetectedVenueRepository repository;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @BeforeEach
    void init() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        repository.saveAll(
                List.of(
                        DetectedVenue.createDetectedVenue(now.minus(1, ChronoUnit.DAYS)),
                        DetectedVenue.createDetectedVenue(now.minus(14, ChronoUnit.DAYS)),
                        DetectedVenue.createDetectedVenue(now.minus(15, ChronoUnit.DAYS))
                )
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
                .filter(cronTask -> cronTask.getExpression().equals("*/5 * * * * *")
                        && cronTask.toString().equals("fr.gouv.tacw.services.impl.PersistService.deleteDetectedVenues"))
                .count();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("check that croned job remove outdated LSPs from DB")
    void deleteDetectedVenues() {
        assertThat(repository.count()).isEqualTo(3);
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(repository.count()).isEqualTo(2));
    }
}