package fr.gouv.clea.ws.utils;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(decoded.getQrCodeScanTime().truncatedTo(ChronoUnit.SECONDS)).isEqualTo(deserializedVisit.getQrCodeScanTime().truncatedTo(ChronoUnit.SECONDS));
        assertThat(decoded.isBackward()).isEqualTo(deserializedVisit.isBackward());
        assertThat(decoded.getEncryptedLocationSpecificPart()).isEqualTo(deserializedVisit.getEncryptedLocationSpecificPart());
        serializer.close();
        deserializer.close();
    }

}