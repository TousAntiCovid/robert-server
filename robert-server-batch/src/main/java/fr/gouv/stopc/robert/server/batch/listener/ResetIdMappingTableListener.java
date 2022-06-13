package fr.gouv.stopc.robert.server.batch.listener;

import fr.gouv.stopc.robert.server.batch.model.ItemProcessingCounterUtils;
import fr.gouv.stopc.robertserver.database.service.ItemIdMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * ItemIdMapping table is used for partitioning in many steps. The table must be
 * reset before performing a new id mapping.
 */
@Slf4j
public class ResetIdMappingTableListener implements StepExecutionListener {

    private final String stepName;

    private final ItemIdMappingService itemIdMappingService;

    public ResetIdMappingTableListener(String stepName, ItemIdMappingService itemIdMappingService) {
        super();
        this.stepName = stepName;
        this.itemIdMappingService = itemIdMappingService;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        resetItemIdMappingCollection();
        log.info("START : {}.", stepName);
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("END : {}.", stepName);
        return stepExecution.getExitStatus();
    }

    protected void resetItemIdMappingCollection() {
        log.info("START : Reset the itemIdMapping collection.");
        ItemProcessingCounterUtils.getInstance().resetCounters();
        itemIdMappingService.deleteAll();
        log.info("END : Reset the itemIdMapping collection.");
    }
}
