package fr.gouv.stopc.robert.server.batch;

import fr.gouv.stopc.robert.server.batch.service.ContactProcessingService;
import fr.gouv.stopc.robert.server.batch.service.HelloMessageService;
import fr.gouv.stopc.robert.server.batch.service.PurgeOldEpochExpositionsService;
import fr.gouv.stopc.robert.server.batch.service.ReassessRiskLevelService;
import fr.gouv.stopc.robert.server.batch.service.RiskEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "robert-batch.command-line-runner.reasses-risk.enabled", havingValue = "true")
@RequiredArgsConstructor
public class RobertCommandLineRunner implements CommandLineRunner {

    private final ReassessRiskLevelService reassessRiskLevelService;

    private final PurgeOldEpochExpositionsService purgeOldEpochExpositionsService;

    private final HelloMessageService helloMessageService;

    private final ContactProcessingService contactProcessingService;

    private final RiskEvaluationService riskEvaluationService;

    @Override
    public void run(String... args) {
        log.info("{} hello messages waiting for process", helloMessageService.getHelloMessageCount());
        purgeOldEpochExpositionsService.performs();
        reassessRiskLevelService.performs();
        contactProcessingService.performs();
        riskEvaluationService.performs();
        log.info("{} hello messages remaining after process", helloMessageService.getHelloMessageCount());
    }
}
