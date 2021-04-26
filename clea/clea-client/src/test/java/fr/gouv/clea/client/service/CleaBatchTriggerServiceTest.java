package fr.gouv.clea.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CleaBatchTriggerServiceTest {
    private CleaBatchTriggerService triggerService;
    
    @Test
    void testTriggerThroughHttpIsCalledWhenProvidingHttpUrl() throws IOException, InterruptedException {
        triggerService = Mockito.spy(new CleaBatchTriggerService("http://foo", null));
        doNothing().when(triggerService).triggerClusterDetectionThroughHttp();
        doNothing().when(triggerService).triggerClusterDetectionThroughSsh();
        
        triggerService.triggerClusterDetection();
        
        verify(triggerService).triggerClusterDetectionThroughHttp();
        verify(triggerService, never()).triggerClusterDetectionThroughSsh();
    }

    @Test
    void testTriggerThroughSshIsCalledWhenProvidingSshUrl() throws IOException, InterruptedException {
        triggerService = Mockito.spy(new CleaBatchTriggerService("ssh://foo"));
        doNothing().when(triggerService).triggerClusterDetectionThroughHttp();
        doNothing().when(triggerService).triggerClusterDetectionThroughSsh();
        
        triggerService.triggerClusterDetection();
        
        verify(triggerService, never()).triggerClusterDetectionThroughHttp();
        verify(triggerService).triggerClusterDetectionThroughSsh();
    }
    
    @Test
    void testCandGetHostFromSshUrl() throws IOException {
        triggerService = new CleaBatchTriggerService("ssh://foo.bar");
        
        assertThat(triggerService.getHost()).isEqualTo("foo.bar");
    }

    @Disabled("This test is intended to be used to trigger manually CLEA batch through SSH")
    @Test
    void testCanTriggerBatchThroughSsh() throws IOException, InterruptedException {
        new CleaBatchTriggerService("ssh://clea-batch.outscale").triggerClusterDetection();
    }
}
