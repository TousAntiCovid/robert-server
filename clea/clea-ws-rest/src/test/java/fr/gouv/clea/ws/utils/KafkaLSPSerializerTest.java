package fr.gouv.clea.ws.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;

class KafkaLSPSerializerTest {

    @Test
    void testCanSerializeAndDeserializeAVisit() {
        DecodedVisit decoded = new DecodedVisit(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                EncryptedLocationSpecificPart.builder()
                        .version(RandomUtils.nextInt())
                        .type(RandomUtils.nextInt())
                        .locationTemporaryPublicId(UUID.randomUUID())
                        .encryptedLocationMessage(RandomUtils.nextBytes(20))
                        .build(),
                RandomUtils.nextBoolean()
        );
        KafkaLSPSerializer serializer = new KafkaLSPSerializer();
        KafkaLSPDeserializer deserializer = new KafkaLSPDeserializer();
        
        byte[] serializedVisit = serializer.serialize("", decoded);
        DecodedVisit deserializedVisit = deserializer.deserialize("", serializedVisit);
        
        assertThat(decoded.getQrCodeScanTime()).isEqualTo(deserializedVisit.getQrCodeScanTime());
        assertThat(decoded.isBackward()).isEqualTo(deserializedVisit.isBackward());
        assertThat(decoded.getEncryptedLocationSpecificPart()).isEqualTo(deserializedVisit.getEncryptedLocationSpecificPart());
        serializer.close();
        deserializer.close();
    }

}