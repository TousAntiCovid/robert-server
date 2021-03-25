package fr.gouv.clea.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.client.model.Cluster;
import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.service.StatusService;

public class StatusTest {
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
        StatusService statusService = new StatusService();
        assertThat(statusService.status(localList)).isGreaterThan(0f);
    }

    @Test
    public void statusShouldReturnFalse() throws Exception {
        List<ScannedQrCode> localList = new ArrayList<>();
        localList.add(qr2);
        List<Cluster> clusters = new ArrayList<>();
        StatusService statusService = new StatusService();
        assertThat(statusService.status(localList)).isEqualTo(0f);
    }
}


