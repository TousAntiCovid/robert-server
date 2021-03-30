package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.CleaEncodingException;
import fr.inria.clea.lsp.CleaEncryptionException;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DecodedVisitServiceTest {

    private final LocationSpecificPartDecoder decoder = mock(LocationSpecificPartDecoder.class);
    private final CleaEciesEncoder cleaEciesEncoder = mock(CleaEciesEncoder.class);
    private final int driftBetweenDeviceAndOfficialTimeInSecs = 300;
    private final int cleaClockDriftInSecs = 300;
    private final IDecodedVisitService decodedVisitService = new DecodedVisitService(decoder, cleaEciesEncoder, driftBetweenDeviceAndOfficialTimeInSecs, cleaClockDriftInSecs);

    @Test
    @DisplayName("check with max CRIexp for LSP")
    void maxCRIexp() throws CleaEncryptionException, CleaEncodingException {
        int CRIexp = 0x1F; // qrCodeRenewalInterval = 0
        Instant now = Instant.now();
        UUID uuid = UUID.randomUUID();
        byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);

        when(decoder.decrypt(any(EncryptedLocationSpecificPart.class)))
                .thenReturn(
                        LocationSpecificPart.builder()
                                .qrCodeRenewalIntervalExponentCompact(CRIexp)
                                .locationTemporaryPublicId(uuid)
                                .locationTemporarySecretKey(locationTemporarySecretKey)
                                .build()
                );

        when(cleaEciesEncoder.computeLocationTemporaryPublicId(locationTemporarySecretKey))
                .thenReturn(uuid);

        Optional<Visit> optional = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        now,
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .build(),
                        RandomUtils.nextBoolean()
                )
        );

        assertThat(optional).isPresent();
    }

    @Disabled
    @Test
    @DisplayName("check with drifting LSP")
    void drift() throws CleaEncryptionException, CleaEncodingException {
        int CRIexp = 10; // qrCodeRenewalInterval=2^10(=1024). 1024+300+300=1624
        Instant now = Instant.now();
        long qrCodeValidityStartTime = TimeUtils.currentNtpTime() + 2000;
        UUID uuid = UUID.randomUUID();
        byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);

        when(decoder.decrypt(any(EncryptedLocationSpecificPart.class)))
                .thenReturn(
                        LocationSpecificPart.builder()
                                .qrCodeValidityStartTime(0)
                                .qrCodeRenewalIntervalExponentCompact(CRIexp)
                                // .qrCodeValidityStartTime(qrCodeValidityStartTime)
                                .locationTemporaryPublicId(uuid)
                                .locationTemporarySecretKey(locationTemporarySecretKey)
                                .build()
                );

        when(cleaEciesEncoder.computeLocationTemporaryPublicId(locationTemporarySecretKey))
                .thenReturn(uuid);

        Optional<Visit> optional = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        now,
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .build(),
                        RandomUtils.nextBoolean()
                )
        );

        assertThat(optional).isEmpty();
    }

    @Test
    @DisplayName("check with non valid temporaryLocationPublicId")
    void nonValidTemporaryLocationPublicId() throws CleaEncryptionException, CleaEncodingException {
        Instant now = Instant.now();
        UUID uuid = UUID.randomUUID();
        UUID _uuid = UUID.randomUUID();
        byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);

        when(decoder.decrypt(any(EncryptedLocationSpecificPart.class)))
                .thenReturn(
                        LocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .locationTemporarySecretKey(locationTemporarySecretKey)
                                .build()
                );

        when(cleaEciesEncoder.computeLocationTemporaryPublicId(locationTemporarySecretKey))
                .thenReturn(_uuid);

        Optional<Visit> optional = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        now,
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .build(),
                        RandomUtils.nextBoolean()
                )
        );

        assertThat(optional).isNotPresent();
    }

    @Test
    @DisplayName("check with CleaEncryptionException when verifying temporaryLocationPublicId")
    void cleaEncryptionExceptionForTLId() throws CleaEncryptionException, CleaEncodingException {
        Instant now = Instant.now();
        UUID uuid = UUID.randomUUID();
        byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);

        when(decoder.decrypt(any(EncryptedLocationSpecificPart.class)))
                .thenReturn(
                        LocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .locationTemporarySecretKey(locationTemporarySecretKey)
                                .build()
                );

        when(cleaEciesEncoder.computeLocationTemporaryPublicId(locationTemporarySecretKey))
                .thenThrow(new CleaEncryptionException(""));

        Optional<Visit> optional = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        now,
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .build(),
                        RandomUtils.nextBoolean()
                )
        );

        assertThat(optional).isNotPresent();
    }

    @Test
    @DisplayName("check with CleaEncryptionException when decrypting")
    void cleaEncryptionException() throws CleaEncryptionException, CleaEncodingException {
        Instant now = Instant.now();
        UUID uuid = UUID.randomUUID();
        byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);

        when(decoder.decrypt(any(EncryptedLocationSpecificPart.class)))
                .thenThrow(new CleaEncryptionException(""));

        when(cleaEciesEncoder.computeLocationTemporaryPublicId(locationTemporarySecretKey))
                .thenReturn(uuid);

        Optional<Visit> optional = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        now,
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .build(),
                        RandomUtils.nextBoolean()
                )
        );

        assertThat(optional).isNotPresent();
    }

    @Test
    @DisplayName("check with CleaEncodingException when decrypting")
    void cleaEncodingException() throws CleaEncryptionException, CleaEncodingException {
        Instant now = Instant.now();
        UUID uuid = UUID.randomUUID();
        byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);

        when(decoder.decrypt(any(EncryptedLocationSpecificPart.class)))
                .thenThrow(new CleaEncodingException(""));

        when(cleaEciesEncoder.computeLocationTemporaryPublicId(locationTemporarySecretKey))
                .thenReturn(uuid);

        Optional<Visit> optional = decodedVisitService.decryptAndValidate(
                new DecodedVisit(
                        now,
                        EncryptedLocationSpecificPart.builder()
                                .locationTemporaryPublicId(uuid)
                                .build(),
                        RandomUtils.nextBoolean()
                )
        );

        assertThat(optional).isNotPresent();
    }
}