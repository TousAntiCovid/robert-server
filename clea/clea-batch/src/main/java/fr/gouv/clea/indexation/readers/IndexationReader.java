package fr.gouv.clea.indexation.readers;

import fr.gouv.clea.dto.SinglePlaceCluster;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fr.gouv.clea.config.BatchConstants.CLUSTERMAP_JOB_CONTEXT_KEY;

@StepScope
public class IndexationReader implements ItemReader<List<SinglePlaceCluster>> {

    private boolean isAlreadyGenerated = false;

    private Map<String, SinglePlaceCluster> clusterMap;

    @BeforeStep
    public void retrieveInterStepData(StepExecution stepExecution) {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        this.clusterMap = (Map<String, SinglePlaceCluster>) jobContext.get(CLUSTERMAP_JOB_CONTEXT_KEY);
    }

    @Override
    public List<SinglePlaceCluster> read() {
        if (!this.isAlreadyGenerated) {
            this.isAlreadyGenerated = true;
            return new ArrayList<>(clusterMap.values());
        }
        this.isAlreadyGenerated = false;
        return null;
    }

}
