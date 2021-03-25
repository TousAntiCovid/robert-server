package fr.gouv.tacw.qr;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import fr.gouv.tacw.qr.model.QRCode;

import org.junit.jupiter.api.BeforeEach;

public class LocationQrCodeGeneratorTest {
    
    private static final String manualContactTracingAuthorityPublicKey  = "04bd776a941090db1c90057401043babafc77164efedad1cbfbab2edec53c5afaff718a33e4cc8f2e9514b162dd4700e517ad341e80f47d49dc0b7e70b30ca4781";
    private static final String permanentLocationSecretKey              = "2d576fddb3b721ef86c1512f1ed95452faa5ec6faba0c7a226ad2ac050ed6d49";
    private static final String serverAuthorityPublicKey                = "04bd776a941090db1c90057401043babafc77164efedad1cbfbab2edec53c5afaff718a33e4cc8f2e9514b162dd4700e517ad341e80f47d49dc0b7e70b30ca4781";
    private static final Instant now = Instant.now();
    private LocationQrCodeGenerator staticGenerator;
    private LocationQrCodeGenerator dynamicGenerator;

    @BeforeEach
    public void setUp() throws Exception{
        staticGenerator = LocationQrCodeGenerator.builder()
                                        .countryCode(250)
                                        .staff(false)
                                        .periodStartTime(now)
                                        .periodDuration(8)
                                        .venueCategory1(0)
                                        .venueCategory2(1)
                                        .venueType(3)
                                        .qrCodeRenewalIntervalExponentCompact(0x1F)
                                        .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                                        .permanentLocationSecretKey(permanentLocationSecretKey)
                                        .serverAuthorityPublicKey(serverAuthorityPublicKey)
                                        .locationPhone("0123456789")
                                        .locationPin("1234")
                                        .build();
        dynamicGenerator = LocationQrCodeGenerator.builder()
                                        .countryCode(250)
                                        .staff(false)
                                        .periodStartTime(now)
                                        .periodDuration(8)
                                        .venueCategory1(0)
                                        .venueCategory2(1)
                                        .venueType(3)
                                        .qrCodeRenewalIntervalExponentCompact(10)
                                        .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                                        .permanentLocationSecretKey(permanentLocationSecretKey)
                                        .serverAuthorityPublicKey(serverAuthorityPublicKey)
                                        .locationPhone("0123456789")
                                        .locationPin("1234")
                                        .build();
    }

    @Test
    public void shouldGenerateQR() throws Exception{
        QRCode qr = staticGenerator.getQrCodeAt(staticGenerator.getPeriodStart());
        assertThat(qr.getQrCode()).isNotEmpty();
    }

    @Test
    public void shouldGenerateOnlyOnce() throws Exception{
        QRCode qr = staticGenerator.getQrCodeAt(staticGenerator.getPeriodStart());
        QRCode qr2 = staticGenerator.getQrCodeAt(staticGenerator.getPeriodStart()+600);
        assertThat(qr2).isEqualTo(qr);
        
        qr = dynamicGenerator.getQrCodeAt(dynamicGenerator.getPeriodStart());
        qr2 = dynamicGenerator.getQrCodeAt(dynamicGenerator.getPeriodStart()+600);
        assertThat(qr2).isEqualTo(qr);
    }
    
    @Test
    public void shouldBeSameTLid() throws Exception{
        QRCode qr = dynamicGenerator.getQrCodeAt(dynamicGenerator.getPeriodStart());
        QRCode qr2 = dynamicGenerator.getQrCodeAt(dynamicGenerator.getPeriodStart()+3600);

        assertThat(qr.getLocationTemporaryPublicID()).isEqualTo(qr2.getLocationTemporaryPublicID());

    }

    @Test
    public void startingNewPeriod() throws Exception{
        QRCode qr = dynamicGenerator.getQrCodeAt(dynamicGenerator.getPeriodStart());
        QRCode qr2 = dynamicGenerator.startNewPeriod(now.plusSeconds(3600*24));
        assertThat(qr).isNotEqualTo(qr2);
        assertThat(qr.getLocationTemporaryPublicID()).isNotEqualTo(qr2.getLocationTemporaryPublicID());
    }
}
