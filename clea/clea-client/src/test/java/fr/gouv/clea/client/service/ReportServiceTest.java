package fr.gouv.clea.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import fr.gouv.clea.client.model.ReportResponse;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.utils.HttpClientWrapper;

public class ReportServiceTest {
    private final String qrCode = "AKSYrwI8hzHkrJv0mf9X3I3a3cz8wvP/zQQZ/uD2cL78m5hBXXW46YrPPTxiYNShhQDvyd6w0zyJD96D0tIy6DIRyQOEuWWxW84GmrMDgiOxCFtWt+qlY1Wnsh1szt4UJpCjkYEf7Ij78n/cEQY=";
    private final String qrCode2 = "AAXpe5EhZz3nv3hF8TtpMguUdtQ3lwlpUG7rG0lu3RtbKJlIIiTpHBllKCkLyrpbRcGTXBtfc3GlO3WsRSxyeBT3ngqYI8sgh7lIMDADHzLI5/V3mf/OiYjOLwurVedWzrrCUG2wkLr8Pc2WuAM=";
    private final Instant now = Instant.ofEpochSecond(3824820600L);
    private ReportService backend;
    private List<ScannedQrCode> localList;

    @BeforeEach
    public void setup() throws IOException, InterruptedException {
        CleaClientConfiguration config = CleaClientConfiguration.getInstance();
        HttpClientWrapper httpClient = mock(HttpClientWrapper.class);
        backend = new ReportService(config.getBackendUrl() + config.getReportPath(), httpClient);
        when(httpClient.post(anyString(),anyString(),any())).thenReturn(new ReportResponse(true, ""));
        localList = new ArrayList<>();
    }

    @Test
    public void testCanReportInfectedVisits() throws Exception {
        localList.add(new ScannedQrCode(qrCode, now.getEpochSecond() - 200));
        localList.add(new ScannedQrCode(qrCode2, now.getEpochSecond()));

        ReportResponse response = backend.report(localList);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEmpty();
    }
}
