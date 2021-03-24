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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class KafkaDeserializer implements Deserializer<DecodedVisit> {

    @Override
    public DecodedVisit deserialize(String topic, byte[] data) {
        if (data == null)
            return null;
        try {
            return new ObjectMapper()
                    .registerModule(new SimpleModule().addDeserializer(DecodedVisit.class, new CustomJacksonDeserializer()))
                    .readValue(data, DecodedVisit.class);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing JSON message", e);
        }
    }
}

class CustomJacksonDeserializer extends StdDeserializer<DecodedVisit> {

    private static final long serialVersionUID = 1L;

    public CustomJacksonDeserializer() {
        this(null);
    }

    public CustomJacksonDeserializer(Class<DecodedVisit> t) {
        super(t);
    }

    @Override
    public DecodedVisit deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        long qrCodeScanTime = node.get("qrCodeScanTime").asLong();
        boolean isBackward = node.get("isBackward").asBoolean();
        int version = node.get("version").asInt();
        int type = node.get("type").asInt();
        UUID locationTemporaryPublicId = UUID.fromString(node.get("locationTemporaryPublicId").asText());
        byte[] encryptedLocationMessage = node.get("encryptedLocationMessage").binaryValue();
        return new DecodedVisit(
                Instant.ofEpochMilli(qrCodeScanTime).truncatedTo(ChronoUnit.SECONDS),
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
