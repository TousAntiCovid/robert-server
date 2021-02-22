package fr.gouv.tacw.database;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.database.utils.TimeUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.TestPropertySource;

import javax.xml.bind.DatatypeConverter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(properties = "tacw.database.scheduling_enabled=true")
public class ExposedStaticVisitServiceScheduledJobTests {

    @Autowired
    ExposedStaticVisitService exposedStaticVisitService;

    @Autowired
    ExposedStaticVisitRepository exposedStaticVisitRepository;

    @Autowired
    private ScheduledTaskHolder scheduledTaskHolder;

    @Value("${tacw.database.visit_token_retention_period_days}")
    private long retentionDays;

    @Value("${tacw.database.visit_token_deletion_job_cron_expression}")
    private String cron;

    @Test
    void testCronIsActive() {
        Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
        scheduledTasks.forEach(scheduledTask -> scheduledTask.getTask().getRunnable().getClass().getDeclaredMethods());
        long count = scheduledTasks.stream()
                .filter(scheduledTask -> scheduledTask.getTask() instanceof CronTask)
                .map(scheduledTask -> (CronTask) scheduledTask.getTask())
                .filter(cronTask -> cronTask.getExpression().equals(cron)
                        && cronTask.toString().equals("fr.gouv.tacw.database.service.ExposedStaticVisitServiceImpl.deleteExpiredTokens"))
                .count();
        assertThat(count).isEqualTo(1L);
    }

    /*
     * we can't change system time at will, so we set cron every 5 seconds for testing purposes
     */
    @Test
    void testScheduledDeletion() throws InterruptedException {
        final List<String> expired_tokens = Stream.of(
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0001",
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0002",
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0003",
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0004",
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0005")
                .collect(Collectors.toList());
        final List<String> valid_tokens = Stream.of(
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1001",
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1002",
                "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1003")
                .collect(Collectors.toList());
        final long currentNtpTime = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
        final long windowStart = currentNtpTime - (retentionDays * 86400);
        exposedStaticVisitService.registerExposedStaticVisitEntities(this.entitiesFrom(expired_tokens, windowStart - 2150));
        exposedStaticVisitService.registerExposedStaticVisitEntities(this.entitiesFrom(valid_tokens, windowStart + 500));
        assertThat(exposedStaticVisitRepository.count()).isEqualTo(expired_tokens.size() + valid_tokens.size());
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(exposedStaticVisitRepository.count()).isEqualTo(valid_tokens.size()));
        for (String token : expired_tokens) {
            assertThat(exposedStaticVisitRepository.findByToken(DatatypeConverter.parseHexBinary(token)).isPresent()).isFalse();
        }
        for (String token : valid_tokens) {
            assertThat(exposedStaticVisitRepository.findByToken(DatatypeConverter.parseHexBinary(token))).isPresent();
        }
    }

    protected List<ExposedStaticVisitEntity> entitiesFrom(List<String> tokens, long visitTime) {
        return tokens.stream()
                .map(token -> this.entityFrom(token, visitTime))
                .collect(Collectors.toList());
    }

    protected ExposedStaticVisitEntity entityFrom(String token, long visitTime) {
        int startDelta = 0;
        int endDelta = 2000;
        long visitStartTime = visitTime - startDelta;
        long visitEndTime = visitTime + endDelta;
        long exposureCount = 1;
        return new ExposedStaticVisitEntity(DatatypeConverter.parseHexBinary(token), RiskLevel.HIGH, visitStartTime, visitEndTime, startDelta, endDelta, exposureCount);
    }
}
