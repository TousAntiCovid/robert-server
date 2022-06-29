package fr.gouv.stopc.robert.server.batch;

import fr.gouv.stopc.robert.server.batch.scheduled.service.ReassessRiskLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(value = 1)
@Component
@ConditionalOnProperty(value = "robert-batch.command-line-runner.reasses-risk.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ReassessRiskLineRunner implements CommandLineRunner {

    private final ReassessRiskLevelService reassessRiskLevelService;

    @Override
    public void run(String... args) {
        reassessRiskLevelService.process();
    }
}
