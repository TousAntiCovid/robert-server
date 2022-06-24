package fr.gouv.stopc.robert.server.batch;

import fr.gouv.stopc.robert.server.batch.scheduled.service.ReassessRiskLevelService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(value = 1)
@Component
@ConditionalOnProperty(value = "spring.commandLineRunner.reassesRisk", havingValue = "on")
public class ReassessRiskLineRunner implements CommandLineRunner {

    private final ReassessRiskLevelService reassessRiskLevelService;

    public ReassessRiskLineRunner(ReassessRiskLevelService reassessRiskLevelService) {
        this.reassessRiskLevelService = reassessRiskLevelService;
    }

    @Override
    public void run(String... args) {
        reassessRiskLevelService.process();
    }
}
