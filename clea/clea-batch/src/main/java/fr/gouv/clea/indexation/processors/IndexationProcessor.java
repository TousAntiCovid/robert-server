package fr.gouv.clea.indexation.processors;

import java.util.HashMap;
import java.util.List;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;

import fr.gouv.clea.config.BatchProperties;
import fr.gouv.clea.dto.SinglePlaceCluster;
import fr.gouv.clea.indexation.model.output.ClusterFile;
import fr.gouv.clea.indexation.model.output.ClusterFileItem;
import fr.gouv.clea.indexation.model.output.Prefix;

@StepScope
public class IndexationProcessor implements ItemProcessor<List<SinglePlaceCluster>, HashMap<String, ClusterFile>> {

    private final int prefixLength;

    public IndexationProcessor(BatchProperties config) {
        this.prefixLength = config.prefixLength;
    }

    @Override
    public HashMap<String, ClusterFile> process(List<SinglePlaceCluster> clusterList) {

        HashMap<String, ClusterFile> clusterIndexMap = new HashMap<>();

        clusterList.forEach(cluster -> {
            final String uuidString = cluster.getLocationTemporaryPublicId().toString();
            final String prefix = Prefix.of(uuidString, prefixLength);
            ClusterFile clusterFile = clusterIndexMap.computeIfAbsent(prefix, key -> new ClusterFile());
            clusterFile.addItem(ClusterFileItem.ofCluster(cluster));
        });

        return clusterIndexMap;
    }
}
