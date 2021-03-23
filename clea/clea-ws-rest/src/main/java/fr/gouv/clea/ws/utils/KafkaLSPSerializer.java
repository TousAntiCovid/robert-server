package fr.gouv.clea.ws.utils;

import java.io.IOException;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;

public class KafkaLSPSerializer implements Serializer<DecodedVisit> {

    @Override
    public byte[] serialize(String topic, DecodedVisit data) {
        if (data == null)
            return null;
        try {
            return new ObjectMapper()
                    .registerModule(new SimpleModule().addSerializer(DecodedVisit.class, new JacksonLSPSerializer()))
                    .writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}

class JacksonLSPSerializer extends StdSerializer<DecodedVisit> {

    private static final long serialVersionUID = 1L;

    public JacksonLSPSerializer() {
        this(null);
    }

    public JacksonLSPSerializer(Class<DecodedVisit> t) {
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
