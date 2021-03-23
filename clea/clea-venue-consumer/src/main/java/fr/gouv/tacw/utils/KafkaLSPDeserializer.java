package fr.gouv.tacw.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import fr.gouv.tacw.dto.DecodedLocationSpecificPart;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.UUID;

public class KafkaLSPDeserializer implements Deserializer<DecodedLocationSpecificPart> {

    @Override
    public DecodedLocationSpecificPart deserialize(String topic, byte[] data) {
        if (data == null)
            return null;
        try {
            return new ObjectMapper()
                    .registerModule(new SimpleModule().addDeserializer(DecodedLocationSpecificPart.class, new JacksonLSPDeserializer()))
                    .readValue(data, DecodedLocationSpecificPart.class);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing JSON message", e);
        }
    }
}

class JacksonLSPDeserializer extends StdDeserializer<DecodedLocationSpecificPart> {

    public JacksonLSPDeserializer() {
        this(null);
    }

    public JacksonLSPDeserializer(Class<DecodedLocationSpecificPart> t) {
        super(t);
    }

    @Override
    public DecodedLocationSpecificPart deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        int version = node.get("version").asInt();
        int type = node.get("type").asInt();
        int countryCode = node.get("countryCode").asInt();
        boolean staff = node.get("staff").asBoolean();
        UUID locationTemporaryPublicId = UUID.fromString(node.get("locationTemporaryPublicId").asText());
        int qrCodeRenewalIntervalExponentCompact = node.get("qrCodeRenewalIntervalExponentCompact").asInt();
        int venueType = node.get("venueType").asInt();
        int venueCategory1 = node.get("venueCategory1").asInt();
        int venueCategory2 = node.get("venueCategory2").asInt();
        int periodDuration = node.get("periodDuration").asInt();
        int compressedPeriodStartTime = node.get("compressedPeriodStartTime").asInt();
        int qrCodeValidityStartTime = node.get("qrCodeValidityStartTime").asInt();
        byte[] locationTemporarySecretKey = node.get("locationTemporarySecretKey").binaryValue();
        byte[] encryptedLocationContactMessage = node.get("encryptedLocationContactMessage").binaryValue();
        long qrCodeScanTime = node.get("qrCodeScanTime").asLong();
        return new DecodedLocationSpecificPart(
                version,
                type,
                countryCode,
                staff,
                locationTemporaryPublicId,
                qrCodeRenewalIntervalExponentCompact,
                venueType,
                venueCategory1,
                venueCategory2,
                periodDuration,
                compressedPeriodStartTime,
                qrCodeValidityStartTime,
                locationTemporarySecretKey,
                encryptedLocationContactMessage,
                qrCodeScanTime
        );
    }
}
