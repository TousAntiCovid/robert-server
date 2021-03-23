package fr.gouv.clea.consumer.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.UUID;

public class KafkaLSPDeserializer implements Deserializer<DecodedVisit> {

    @Override
    public DecodedVisit deserialize(String topic, byte[] data) {
        if (data == null)
            return null;
        try {
            return new ObjectMapper()
                    .registerModule(new SimpleModule().addDeserializer(DecodedVisit.class, new JacksonLSPDeserializer()))
                    .readValue(data, DecodedVisit.class);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing JSON message", e);
        }
    }
}

class JacksonLSPDeserializer extends StdDeserializer<DecodedVisit> {

    public JacksonLSPDeserializer() {
        this(null);
    }

    public JacksonLSPDeserializer(Class<DecodedVisit> t) {
        super(t);
    }

    @Override
    public DecodedVisit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        long qrCodeScanTime = node.get("qrCodeScanTime").asLong();
        boolean isBackward = node.get("isBackward").asBoolean();
        int version = node.get("version").asInt();
        int type = node.get("type").asInt();
        UUID locationTemporaryPublicId = UUID.fromString(node.get("locationTemporaryPublicId").asText());
        byte[] encryptedLocationMessage = node.get("encryptedLocationMessage").binaryValue();
        return new DecodedVisit(
                qrCodeScanTime,
                EncryptedLocationSpecificPart.builder()
                        .version(version)
                        .type(type)
                        .locationTemporaryPublicId(locationTemporaryPublicId)
                        .encryptedLocationMessage(encryptedLocationMessage)
                        .build(),
                isBackward
        );
    }
}
