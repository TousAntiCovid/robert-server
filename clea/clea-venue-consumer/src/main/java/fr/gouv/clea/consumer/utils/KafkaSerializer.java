package fr.gouv.clea.consumer.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

public class KafkaSerializer implements Serializer<DecodedVisit> {

    @Override
    public byte[] serialize(String topic, DecodedVisit data) {
        if (data == null)
            return null;
        try {
            return new ObjectMapper()
                    .registerModule(new SimpleModule().addSerializer(DecodedVisit.class, new CustomJacksonSerializer()))
                    .writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}

class CustomJacksonSerializer extends StdSerializer<DecodedVisit> {

    private static final long serialVersionUID = 1L;

    public CustomJacksonSerializer() {
        this(null);
    }

    public CustomJacksonSerializer(Class<DecodedVisit> t) {
        super(t);
    }

    @Override
    public void serialize(DecodedVisit visit, JsonGenerator generator, SerializerProvider provider) throws IOException {
        long qrCodeScanTime = visit.getQrCodeScanTime().toEpochMilli();
        boolean isBackward = visit.isBackward();
        EncryptedLocationSpecificPart enc = visit.getEncryptedLocationSpecificPart();
        generator.writeStartObject();
        generator.writeNumberField("qrCodeScanTime", qrCodeScanTime);
        generator.writeBooleanField("isBackward", isBackward);
        generator.writeNumberField("version", enc.getVersion());
        generator.writeNumberField("type", enc.getType());
        generator.writeStringField("locationTemporaryPublicId", enc.getLocationTemporaryPublicId().toString());
        generator.writeBinaryField("encryptedLocationMessage", enc.getEncryptedLocationMessage());
        generator.writeEndObject();
    }
}
