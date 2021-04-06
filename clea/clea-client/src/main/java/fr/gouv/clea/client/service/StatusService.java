package fr.gouv.clea.client.service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import fr.gouv.clea.client.model.Cluster;
import fr.gouv.clea.client.model.ClusterExposition;
import fr.gouv.clea.client.model.ClusterIndex;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.utils.ContentReader;
import fr.gouv.clea.client.utils.FileContentReader;
import fr.gouv.clea.client.utils.UrlContentReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatusService {

    private String indexPath;
    private String indexFilename;
    private ObjectMapper objectMapper;
    private ContentReader contentReader;

    public StatusService(CleaClientConfiguration config) {
        this.indexPath = config.getStatusPath();
        this.indexFilename = config.getIndexFilename();
        if (this.indexPath.startsWith("http")) {
            this.contentReader = new UrlContentReader();
        } else {
            this.contentReader = new FileContentReader();
        }
        objectMapper = new ObjectMapper();
    }

    /**
     * Retrieve the cluster list and compare it to the local list to compute the
     * risk score, returning the risk Level.
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
            List<Cluster> clusters = this.readClusterFile(Integer.toString(iteration), prefix + ".json");
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
        Optional<String> indexString = Optional.empty();
        URI clusterIndexUri = this.contentReader.uriFrom(this.indexPath, this.indexFilename);
        indexString = this.contentReader.getContent(clusterIndexUri);

        if (indexString.isEmpty()) {
            return Optional.empty();
        }
        try {
            ClusterIndex clusterIndex = objectMapper.readValue(indexString.get(), ClusterIndex.class);
            return Optional.of(clusterIndex);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to create ClusterIndex Object.", e);
            return Optional.empty();
        }
    }
    
    protected List<Cluster> readClusterFile(String... segments) throws JsonMappingException, JsonProcessingException {
        Optional<String> clusterFileString = Optional.empty();
        URI clusterIndexUri = this.contentReader.uriFrom(this.indexPath, segments);
        clusterFileString = this.contentReader.getContent(clusterIndexUri);
        return objectMapper.readValue(clusterFileString.get(), new TypeReference<List<Cluster>>() {});
    }

}
