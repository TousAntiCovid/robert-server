package fr.gouv.stopc.robert.server.batch.tasklet;

import fr.gouv.stopc.robertserver.database.repository.KpiRepository;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SaveKpisTasklet implements Tasklet {

    private final RegistrationRepository registrationRepository;

    private final KpiRepository kpiRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        log.info("Updating batch kpis");

        final var exposedUsersNotAtRisk = registrationRepository.countNbExposedUsersButNotAtRisk();
        final var infectedUsersNotNotified = registrationRepository.countNbUsersAtRiskAndNotNotified();
        final var notifiedUsersScoredAgain = registrationRepository.countNbNotifiedUsersScoredAgain();

        kpiRepository.upsert("exposedUsersNotAtRisk", exposedUsersNotAtRisk);
        log.info("Updated {} kpi to {}", "exposedUsersNotAtRisk", exposedUsersNotAtRisk);
        kpiRepository.upsert("infectedUsersNotNotified", infectedUsersNotNotified);
        log.info("Updated {} kpi to {}", "infectedUsersNotNotified", infectedUsersNotNotified);
        kpiRepository.upsert("notifiedUsersScoredAgain", notifiedUsersScoredAgain);
        log.info("Updated {} kpi to {}", "notifiedUsersScoredAgain", notifiedUsersScoredAgain);

        return RepeatStatus.FINISHED;
    }

}
