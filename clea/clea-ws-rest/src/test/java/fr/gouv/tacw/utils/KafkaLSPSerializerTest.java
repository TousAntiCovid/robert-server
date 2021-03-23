package fr.gouv.tacw.utils;

import fr.gouv.tacw.dtos.DecodedLocationSpecificPart;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaLSPSerializerTest {

    @Test
    void serialize() {
        DecodedLocationSpecificPart decoded = new DecodedLocationSpecificPart(
                0,
                0,
                0,
                false,
                UUID.randomUUID(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                RandomUtils.nextBytes(20),
                RandomUtils.nextBytes(20),
                0,
                RandomStringUtils.randomAlphanumeric(20)
        );
        KafkaLSPSerializer serializer = new KafkaLSPSerializer();
        KafkaLSPDeserializer deserializer = new KafkaLSPDeserializer();
        byte[] ser = serializer.serialize("", decoded);
        DecodedLocationSpecificPart deSer = deserializer.deserialize("", ser);
        assertThat(decoded).isEqualTo(deSer);
    }
}