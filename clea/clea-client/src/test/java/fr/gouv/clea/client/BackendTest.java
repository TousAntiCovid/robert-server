package fr.gouv.clea.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.client.model.Cluster;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.service.BackendService;
import fr.gouv.clea.client.utils.HttpClientWrapper;

public class BackendTest {
    private final String qrCode = "AKSYrwI8hzHkrJv0mf9X3I3a3cz8wvP/zQQZ/uD2cL78m5hBXXW46YrPPTxiYNShhQDvyd6w0zyJD96D0tIy6DIRyQOEuWWxW84GmrMDgiOxCFtWt+qlY1Wnsh1szt4UJpCjkYEf7Ij78n/cEQY=";
    private final String tlId = "pJivAjyHMeSsm/SZ/1fcjQ==";
    private final String qrCode2 = "AAXpe5EhZz3nv3hF8TtpMguUdtQ3lwlpUG7rG0lu3RtbKJlIIiTpHBllKCkLyrpbRcGTXBtfc3GlO3WsRSxyeBT3ngqYI8sgh7lIMDADHzLI5/V3mf/OiYjOLwurVedWzrrCUG2wkLr8Pc2WuAM=";
    private final String tlId2 = "Bel7kSFnPee/eEXxO2kyCw==";
    private final Instant now = Instant.ofEpochSecond(3824820600L);
    private ScannedQrCode qr;
    private ScannedQrCode qr2;

    @BeforeEach
    public void setup(){
        qr = new ScannedQrCode(qrCode, now.getEpochSecond());
        qr2 = new ScannedQrCode(qrCode2, now.getEpochSecond());
    }
    @Test
    public void statusShouldReturnTrue() throws Exception {
        List<ScannedQrCode> localList = new ArrayList<>();
        localList.add(qr);
        BackendService backend = new BackendService();
        assertThat(backend.status(localList)).isGreaterThan(0f);
    }

    @Test
    public void statusShouldReturnFalse() throws Exception {
        List<ScannedQrCode> localList = new ArrayList<>();
        localList.add(qr2);
        List<Cluster> clusters = new ArrayList<>();
        BackendService backend = new BackendService();
        assertThat(backend.status(localList)).isEqualTo(0f);
    }

    @Test
    public void reportShouldReturnTrue() throws Exception {
        List<ScannedQrCode> localList = new ArrayList<>();
        ScannedQrCode qr = new ScannedQrCode(qrCode, now.getEpochSecond());
        localList.add(qr);
        HttpClientWrapper client = mock(HttpClientWrapper.class);
        when(client.postStatusCode(anyString(), anyString())).thenReturn(200);
        BackendService backend = new BackendService(client);

        assertThat(backend.report(localList)).isTrue();
    }

    @Test
    public void reportShouldReturnFalse() throws Exception {
        List<ScannedQrCode> localList = new ArrayList<>();
        ScannedQrCode qr = new ScannedQrCode(qrCode, now.getEpochSecond());
        localList.add(qr);
        HttpClientWrapper client = mock(HttpClientWrapper.class);
        when(client.postStatusCode(anyString(), anyString())).thenReturn(500);
        BackendService backend = new BackendService(client);

        assertThat(backend.report(localList)).isFalse();
    }
}
