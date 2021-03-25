package fr.gouv.clea.client.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import fr.gouv.clea.client.model.Cluster;
import fr.gouv.clea.client.model.ClusterExposition;
import fr.gouv.clea.client.model.ClusterIndex;
import fr.gouv.clea.client.model.Report;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.utils.HttpClientWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackendService {
    private HttpClientWrapper client;
    private CleaClientConfiguration config;

    public BackendService() throws IOException {
        this.client = new HttpClientWrapper();
        this.config = CleaClientConfiguration.getInstance();
    }

    public BackendService(HttpClientWrapper client) throws IOException {
        this();
        this.client = client;
    }

    /**
     * Retrieve the cluster list and compare it to the
     * local list to compute the risk score, returning true if at risk, false
     * otherwise.
     * 
     * @param localList : LocalList of the simulated device, containing all the
     *                  scanned QR code
     * @return true if this localList is at risk, false otherwise
     */
    public float status(List<ScannedQrCode> localList) {
        Set<String> matchingPrefixes = new HashSet<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String indexStr;
        try {
            indexStr = Files.readString(Path.of(config.getStatusPath(), config.getIndexFilename()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error retrieving index file.");
            return 0;
        }
        ClusterIndex clusterIndex;
        try {
            clusterIndex = objectMapper.readValue(indexStr, ClusterIndex.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON to create ClusterIndex Object.");
            return 0;
        }
        
        for(String prefix : clusterIndex.getPrefixes()){
            for(ScannedQrCode qr : localList){
                if(qr.startWithPrefix(prefix)){
                    matchingPrefixes.add(prefix);
                    break;
                }
            }
        }
        int iteration = clusterIndex.getIteration();
        List<Float> scores = new ArrayList<>();
        // gather all potential clusters
        for(String prefix : matchingPrefixes){
            String clustersString;
            Path currentPath = Path.of(config.getStatusPath(),Integer.toString(iteration), prefix+".json");
            try {
                clustersString = Files.readString(currentPath, StandardCharsets.UTF_8);
                List<Cluster> clusters = objectMapper.readValue(clustersString, new TypeReference<List<Cluster>>(){});
                for(Cluster cluster : clusters){
                    for(ScannedQrCode qr : localList){
                        if(qr.getLocationTemporaryId().equals(cluster.getLocationTemporaryPublicID())){
                            for(ClusterExposition exposition : cluster.getExpositions()){
                                if(exposition.isInExposition(qr.getScanTime())){
                                    scores.add(exposition.getRisk());
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Could not open file :"+currentPath.toString() +" computed score might be false.");
                continue;
            }

        }
        
        return scores.stream().max(Comparator.naturalOrder()).orElse(0f);
    }

    /**
     * report a list of qr code to the backend server
     * 
     * @param localList : list of the qr code to report
     * @return true if report was done (status code == 200) false otherwise
     */
    public boolean report(List<ScannedQrCode> localList) {
        String jsonRequest;
        Report reportRequest = new Report();
        reportRequest.addAllVisits(localList);
        
        try {
            jsonRequest = new ObjectMapper().writeValueAsString(reportRequest);
        } catch (JsonProcessingException e) {
            log.error("Error Creating JSON String.");
            return false;
        }
        String uri = config.getBackendUrl() + config.getReportPath();
        try {
            int response = this.client.postStatusCode(uri, jsonRequest);
            if (response == 200)
                return true;
            else
                return false;
        } catch (IOException | InterruptedException e) {
            log.error("Error during report HTTP request.");
            return false;
        }
    }

}
