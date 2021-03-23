package fr.gouv.clea.ws.utils;

import fr.gouv.clea.ws.model.SerializableDecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaLSPSerializerTest {

    @Test
    void serialize() {
        SerializableDecodedVisit decoded = new SerializableDecodedVisit(
                RandomUtils.nextLong(),
                EncryptedLocationSpecificPart.builder()
                        .version(RandomUtils.nextInt())
                        .type(RandomUtils.nextInt())
                        .locationTemporaryPublicId(UUID.randomUUID())
                        .encryptedLocationMessage(RandomUtils.nextBytes(20))
                        .build(),
                RandomUtils.nextLong()
        );
        KafkaLSPSerializer serializer = new KafkaLSPSerializer();
        KafkaLSPDeserializer deserializer = new KafkaLSPDeserializer();
        byte[] ser = serializer.serialize("", decoded);
        SerializableDecodedVisit deSer = deserializer.deserialize("", ser);
        assertThat(decoded.getQrCodeScanTime()).isEqualTo(deSer.getQrCodeScanTime());
        assertThat(decoded.getPivotDate()).isEqualTo(deSer.getPivotDate());
        assertThat(decoded.getEncryptedLocationSpecificPart()).isEqualTo(deSer.getEncryptedLocationSpecificPart());
    }

}