package fr.gouv.clea.client.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import fr.gouv.clea.client.model.ScannedQrCode;

public class StatusServiceTest {
    private final String qrCode = "AKSYrwI8hzHkrJv0mf9X3I3a3cz8wvP_zQQZ_uD2cL78m5hBXXW46YrPPTxiYNShhQDvyd6w0zyJD96D0tIy6DIRyQOEuWWxW84GmrMDgiOxCFtWt-qlY1Wnsh1szt4UJpCjkYEf7Ij78n_cEQY";
    private final String qrCode2 = "AAXpe5EhZz3nv3hF8TtpMguUdtQ3lwlpUG7rG0lu3RtbKJlIIiTpHBllKCkLyrpbRcGTXBtfc3GlO3WsRSxyeBT3ngqYI8sgh7lIMDADHzLI5_V3mf_OiYjOLwurVedWzrrCUG2wkLr8Pc2WuAM";
    private final Instant now = Instant.ofEpochSecond(1615831800L);
    private ScannedQrCode qr;
    private ScannedQrCode qr2;
    private StatusService statusService;

    @BeforeEach
    public void setup() throws Exception {
        System.out.println(now);
        qr = new ScannedQrCode(qrCode, now);
        qr2 = new ScannedQrCode(qrCode2, now);
        statusService = new StatusService(CleaClientConfiguration.getInstance());
    }
    
    @Test
    public void statusShouldReturnAtRisk() throws IOException  {
        List<ScannedQrCode> localList = new ArrayList<>();
        localList.add(qr2);
        assertThat(statusService.status(localList)).isGreaterThan(0f);
    }

    @Test
    public void statusShouldReturnNoRisk() throws IOException {
        List<ScannedQrCode> localList = new ArrayList<>();
        localList.add(qr);
        assertThat(statusService.status(localList)).isEqualTo(0f);
    }
}


