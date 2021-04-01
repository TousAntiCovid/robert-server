package fr.gouv.clea.identification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

import fr.gouv.clea.dto.SinglePlaceCluster;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SinglePlaceExposedVisitsItemWriter implements ItemWriter<SinglePlaceCluster> {

    private Map<UUID, SinglePlaceCluster> clusterMap;
    
    @BeforeStep
    public void saveStepExecution(StepExecution stepExecution) {
		stepExecution.getJobExecution().getExecutionContext().put("clusterMap", clusterMap = new ConcurrentHashMap<>());
    }

    @Override
    public void write(List<? extends SinglePlaceCluster> list) {
        list.forEach(singlePlaceCluster -> {
        	log.debug("Adding to clusterMap: {}",singlePlaceCluster.getLocationTemporaryPublicId());
        	clusterMap.put(singlePlaceCluster.getLocationTemporaryPublicId(), singlePlaceCluster);
        });
    }
}
