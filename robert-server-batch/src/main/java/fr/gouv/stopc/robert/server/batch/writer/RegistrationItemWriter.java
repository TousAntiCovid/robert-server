package fr.gouv.stopc.robert.server.batch.writer;

import fr.gouv.stopc.robert.server.batch.model.ItemProcessingCounterUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class RegistrationItemWriter implements ItemWriter<Registration> {

    private IRegistrationService registrationService;

    private Long totalRegistrationCount;

    private String totalItemCountKey;

    public RegistrationItemWriter(IRegistrationService registrationService, String totalItemCountKey) {
        this.registrationService = registrationService;
        this.totalItemCountKey = totalItemCountKey;
    }

    @Override
    public void write(List<? extends Registration> items) {

        if (!CollectionUtils.isEmpty(items)) {
            log.info("== Start of the update of the Registrations ==");

            updateRegistrationList((List<Registration>) items);

            log.info("== End of the update of the Registrations ==");
        } else {
            log.info("== There aren't any Registration to update.");
        }
    }

    private void updateRegistrationList(List<Registration> registrationList) {
        Instant startTime = Instant.now();

        log.info("{} Registration(s) to update.", registrationList.size());
        registrationService.saveAll(registrationList);

        long timeElapsed = Duration.between(startTime, Instant.now()).getSeconds();
        int processedRegistrationCount = ItemProcessingCounterUtils.getInstance()
                .addNumberOfProcessedRegistrations(registrationList.size());

        log.info("Execution duration of the update registrations : {} second(s).", timeElapsed);
        log.info(
                "Total number of updated registrations/Total number of registrations to update : {}/{}",
                processedRegistrationCount, totalRegistrationCount
        );
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        totalRegistrationCount = stepExecution.getJobExecution().getExecutionContext().getLong(totalItemCountKey);
    }
}
