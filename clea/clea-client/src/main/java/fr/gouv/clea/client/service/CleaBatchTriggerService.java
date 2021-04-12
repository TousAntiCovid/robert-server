package fr.gouv.clea.client.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import fr.gouv.clea.client.utils.HttpClientWrapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CleaBatchTriggerService {
    private static final String BATCH_TRIGGER_COMMAND = "sudo systemctl start clea-batch";

    private HttpClientWrapper httpClient;
    private String batchTriggerUrl;

    public CleaBatchTriggerService(String batchTriggerUrl) throws IOException {
        this(batchTriggerUrl, new HttpClientWrapper());
    }

    public CleaBatchTriggerService(String batchTriggerEndPoint, HttpClientWrapper httpClientWrapper) {
        this.httpClient = httpClientWrapper;
        this.batchTriggerUrl = batchTriggerEndPoint;
    }
    
    /**
     * Trigger the Cluster detection batch of Clea Server.
     */
    public void triggerClusterDetection() throws IOException, InterruptedException {
        if (this.batchTriggerUrl.startsWith("http")) {
            this.triggerClusterDetectionThroughHttp();
        } else if (this.batchTriggerUrl.startsWith("ssh")) {
            this.triggerClusterDetectionThroughSsh();
        } else {
            throw new RuntimeException("Invalid batch trigger URL!");
        }
    }
    
    public void triggerClusterDetectionThroughHttp() throws IOException, InterruptedException {        
        log.info("Triggering cluster detection on {}",  this.batchTriggerUrl);
        int statusCode = httpClient.postStatusCode(this.batchTriggerUrl, "");
        if (statusCode != 200) {
            throw new RuntimeException("Error when sending triggerClusterDetection request");
        }
    }
    
    public void triggerClusterDetectionThroughSsh() throws IOException, InterruptedException {     
        final String remoteHost = getHost();
        log.info("Triggering cluster detection on {}", remoteHost);
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("ssh", remoteHost, BATCH_TRIGGER_COMMAND);
        builder.directory(new File(System.getProperty("user.home")));
        log.debug("Running command " + builder.command().toString());
        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), log::debug);
        Executors.newSingleThreadExecutor().submit(streamGobbler);
        boolean hasExited = process.waitFor(2, TimeUnit.SECONDS);
        if (!hasExited) {
            throw new RuntimeException("Cluster detection trigger timeout (ssh)");
    }
        if (process.exitValue() != 0) {
            throw new RuntimeException("Cluster detection trigger failed (ssh)");
        }
    }

    protected String getHost() {
        return URI.create(this.batchTriggerUrl).getHost();
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
              .forEach(consumer);
        }
    }
}
