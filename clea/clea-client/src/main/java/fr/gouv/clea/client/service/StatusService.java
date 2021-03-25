package fr.gouv.clea.client.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import fr.gouv.clea.client.model.Cluster;
import fr.gouv.clea.client.model.ClusterExposition;
import fr.gouv.clea.client.model.ClusterIndex;
import fr.gouv.clea.client.model.ScannedQrCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatusService {

    private String indexPath;
    private String indexFilename;
    private ObjectMapper objectMapper;

    public StatusService() throws IOException {
        CleaClientConfiguration config = CleaClientConfiguration.getInstance();
        this.indexPath = config.getStatusPath();
        this.indexFilename = config.getIndexFilename();
        objectMapper = new ObjectMapper();
    }

    /**
     * Retrieve the cluster list and compare it to the local list to compute the
     * risk score, returning true if at risk, false otherwise.
     * 
     * @param localList : LocalList of the simulated device, containing all the
     *                  scanned QR code
     * @return true if this localList is at risk, false otherwise
     * @throws IOException 
     */
    public float status(List<ScannedQrCode> localList) throws IOException {
        ClusterIndex clusterIndex = this.getClusterIndex().orElseThrow();
        Set<String> matchingPrefixes = this.getClusterFilesMatchingPrefix(localList, clusterIndex);
        int iteration = clusterIndex.getIteration();
        List<Float> scores = new ArrayList<>();
        // gather all potential clusters
        for (String prefix : matchingPrefixes) {
            Path currentPath = Path.of(this.indexPath, Integer.toString(iteration), prefix + ".json");
            List<Cluster> clusters = this.readClusterFile(currentPath);
                for (Cluster cluster : clusters) {
                    for (ScannedQrCode qr : localList) {
                        this.getQrRiskLevel(qr, cluster).ifPresent(risk -> scores.add(risk));
                    }
                }
        }

        return scores.stream().max(Comparator.naturalOrder()).orElse(0f);
    }

    protected Optional<Float> getQrRiskLevel(ScannedQrCode qr, Cluster cluster) throws IOException {
        Optional<Float> result = Optional.empty();
        if (qr.getLocationTemporaryId().equals(cluster.getLocationTemporaryPublicID())) {
            for (ClusterExposition exposition : cluster.getExpositions()) {
                if (exposition.isInExposition(qr.getScanTime())) {
                    float newRisk = Math.max(result.orElse(0f), exposition.getRisk());
                    result = Optional.of(newRisk);
                }
            }
        }
        return result;
    }

    protected Set<String> getClusterFilesMatchingPrefix(List<ScannedQrCode> localList, ClusterIndex clusterIndex) {
        Set<String> matchingPrefixes = new HashSet<>();
        for (String prefix : clusterIndex.getPrefixes()) {
            for (ScannedQrCode qr : localList) {
                if (qr.startWithPrefix(prefix)) {
                    matchingPrefixes.add(prefix);
                    break;
                }
            }
        }
        return matchingPrefixes;
    }

    protected Optional<ClusterIndex> getClusterIndex() {
        String indexString;
        try {
            indexString = Files.readString(Path.of(this.indexPath, this.indexFilename), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error retrieving index file.", e);
            return Optional.empty();
        }
        ClusterIndex clusterIndex;
        try {
            clusterIndex = objectMapper.readValue(indexString, ClusterIndex.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to create ClusterIndex Object.", e);
            return Optional.empty();
        }
        return Optional.of(clusterIndex);
    }

    protected List<Cluster> readClusterFile(Path path) {
        try {
            String clustersString = Files.readString(path, StandardCharsets.UTF_8);
            return objectMapper.readValue(clustersString, new TypeReference<List<Cluster>>() {});
        } catch (IOException e) {
            log.error("Could not open file :" + path.toString() + " computed score might be false.", e);
            return Collections.emptyList();
        }
    
    }

}
