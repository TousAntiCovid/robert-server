package fr.gouv.tacw.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.gouv.tacw.data.DecodedLocationSpecificPart;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

public class KafkaLSPSerializer implements Serializer<DecodedLocationSpecificPart> {

    @Override
    public byte[] serialize(String topic, DecodedLocationSpecificPart data) {
        if (data == null)
            return null;
        try {
            return new ObjectMapper()
                    .registerModule(new SimpleModule().addSerializer(DecodedLocationSpecificPart.class, new JacksonLSPSerializer()))
                    .writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}

class JacksonLSPSerializer extends StdSerializer<DecodedLocationSpecificPart> {

    public JacksonLSPSerializer() {
        this(null);
    }

    public JacksonLSPSerializer(Class<DecodedLocationSpecificPart> t) {
        super(t);
    }

    @Override
    public void serialize(DecodedLocationSpecificPart value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("version", value.getVersion());
        gen.writeNumberField("type", value.getType());
        gen.writeNumberField("countryCode", value.getCountryCode());
        gen.writeBooleanField("staff", value.isStaff());
        gen.writeStringField("locationTemporaryPublicId", value.getLocationTemporaryPublicId().toString());
        gen.writeNumberField("qrCodeRenewalIntervalExponentCompact", value.getQrCodeRenewalIntervalExponentCompact());
        gen.writeNumberField("venueType", value.getVenueType());
        gen.writeNumberField("venueCategory1", value.getVenueCategory1());
        gen.writeNumberField("venueCategory2", value.getVenueCategory2());
        gen.writeNumberField("periodDuration", value.getPeriodDuration());
        gen.writeNumberField("compressedPeriodStartTime", value.getCompressedPeriodStartTime());
        gen.writeNumberField("qrCodeValidityStartTime", value.getQrCodeValidityStartTime());
        gen.writeBinaryField("locationTemporarySecretKey", value.getLocationTemporarySecretKey());
        gen.writeBinaryField("encryptedLocationContactMessage", value.getEncryptedLocationContactMessage());
        gen.writeNumberField("qrCodeScanTime", value.getQrCodeScanTime());
        gen.writeEndObject();
    }
}
