package fr.gouv.clea.client.service;

import java.io.IOException;

import fr.gouv.clea.client.utils.HttpClientWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CleaBatchTriggerService {
    private HttpClientWrapper httpClient;
    private String batchTriggerEndPoint;

    public CleaBatchTriggerService(String batchTriggerEndPoint) throws IOException {
        this(batchTriggerEndPoint, new HttpClientWrapper());
    }

    public CleaBatchTriggerService(String batchTriggerEndPoint, HttpClientWrapper httpClientWrapper) {
        this.httpClient = httpClientWrapper;
        this.batchTriggerEndPoint = batchTriggerEndPoint;
    }
    
    /**
     * Trigger the Cluster detection batch of Clea Server.
     */
    public void triggerClusterDetection() throws IOException, InterruptedException {        
        log.info("Triggering cluster detection on {}",  this.batchTriggerEndPoint);
        int statusCode = httpClient.getStatusCode(this.batchTriggerEndPoint);
        if (statusCode != 200) {
            throw new RuntimeException("Error when sending triggerClusterDetection request");
        }
    }
}
