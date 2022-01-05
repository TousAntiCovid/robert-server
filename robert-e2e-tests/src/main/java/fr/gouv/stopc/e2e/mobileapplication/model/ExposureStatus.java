package fr.gouv.stopc.e2e.mobileapplication.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.robert.client.model.ExposureStatusResponse;
import lombok.Value;

import java.time.Instant;
import java.util.Base64;

import static fr.gouv.stopc.e2e.external.common.utils.TimeUtils.convertNTPSecondsToUnixMillis;
import static java.lang.Long.parseLong;
import static java.time.Instant.ofEpochMilli;

@Value
public class ExposureStatus {

    Instant lastContactDate;

    Integer riskLevel;

    Instant cnamLastContactDate;

    public ExposureStatus(ExposureStatusResponse exposureStatusResponse) {
        riskLevel = exposureStatusResponse.getRiskLevel();
        var jwtPayload = decryptJwt(exposureStatusResponse.getDeclarationToken());
        Instant cnamLastContact = null;
        if (null != jwtPayload) {
            cnamLastContact = ofEpochMilli(
                    convertNTPSecondsToUnixMillis(parseLong(jwtPayload.getLastContactDateTimestamp()))
            );
        }
        cnamLastContactDate = cnamLastContact;
        Instant lastContact = null;
        if (null != exposureStatusResponse.getLastContactDate()) {
            lastContact = ofEpochMilli(
                    convertNTPSecondsToUnixMillis(parseLong(exposureStatusResponse.getLastContactDate()))
            );
        }
        lastContactDate = lastContact;
    }

    public Payload decryptJwt(String jwt) {
        if (null != jwt) {
            final String[] chunks = jwt.split("\\.");
            final Base64.Decoder decoder = Base64.getDecoder();
            final String payloadString = new String(decoder.decode(chunks[1]));
            final ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readValue(payloadString, Payload.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

}
