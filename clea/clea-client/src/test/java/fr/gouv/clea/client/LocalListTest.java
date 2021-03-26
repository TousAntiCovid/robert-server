package fr.gouv.clea.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.gouv.clea.client.model.ScannedQrCode;
import fr.gouv.clea.client.service.CleaClient;

/**
 * Basic Unit Test for Client
 */
public class LocalListTest 
{
    private final String prefix = "https://tac.gouv.fr/";
    private final String qrCode = "AKSYrwI8hzHkrJv0mf9X3I3a3cz8wvP/zQQZ/uD2cL78m5hBXXW46YrPPTxiYNShhQDvyd6w0zyJD96D0tIy6DIRyQOEuWWxW84GmrMDgiOxCFtWt+qlY1Wnsh1szt4UJpCjkYEf7Ij78n/cEQY=";
    private final String qrCode2 = "AKSYrwI8hzHkrJv0mf9X3I0KXTn4TUzSX7aM4pfWCpsb7CPSLULz1FBWh9+7RP0hU0VxTb15uDJXY61itwy9yJzDbkz8FGXUZra0LBwCg3D8EbSZsBk/g/havNababZULUxXs8IEaMaims2BnOY=";
    private final String tlId = "pJivAjyHMeSsm/SZ/1fcjQ==";
    private final Instant now = Instant.now(); 
    
    /**
     * Test : simulating Scanning a QR code and saving it to the LocalList
     */
    @Test
    public void shouldAddScannedQrCode()
    {
        CleaClient cleaClient = new CleaClient("alice");
        
        cleaClient.scanQrCode(prefix.concat(qrCode), now.getEpochSecond());
        
        List<ScannedQrCode> localList = cleaClient.getLocalList();
        assertThat(localList.size()).isEqualTo(1);
        ScannedQrCode scanned = localList.get(0);
        assertThat(scanned.getQrCode()).isEqualTo(qrCode);
        assertThat(scanned.getScanTime()).isEqualTo(now.getEpochSecond());
        assertThat(scanned.getLocationTemporaryId()).isEqualTo(tlId);
    }

     /**
     * Test : scanning a qr code with the same Tlid before DUPTHRESHOLD second should not be added to the list
     */
    @Test
    public void shouldNotAddTwice(){
        CleaClient cleaClient = new CleaClient("bob");
        
        cleaClient.scanQrCode(prefix.concat(qrCode), now.getEpochSecond());
        cleaClient.scanQrCode(prefix.concat(qrCode2),now.plusSeconds(3600).getEpochSecond());
        
        List<ScannedQrCode> localList = cleaClient.getLocalList();
        assertThat(localList.size()).isEqualTo(1);
    }

     /**
     * Test : scanning a qr code with the same Tlid after DUPTHRESHOLD second should be added to the list
     */
    @Test
    public void shouldAddTwice(){
        CleaClient cleaClient = new CleaClient("alice");
        
        cleaClient.scanQrCode(prefix.concat(qrCode), now.getEpochSecond());
        cleaClient.scanQrCode(prefix.concat(qrCode2),now.plusSeconds(4*3600).getEpochSecond());
        
        List<ScannedQrCode> localList = cleaClient.getLocalList();
        assertThat(localList.size()).isEqualTo(2);
    }
}
