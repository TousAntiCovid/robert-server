package fr.gouv.stopc.e2e.mobileapplication.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Payload {

    @JsonProperty("jti")
    String jti;

    @JsonProperty("iat")
    String iat;

    @JsonProperty("iss")
    String iss;

    @JsonProperty("notificationDateTimestamp")
    String notificationDateTimestamp;

    @JsonProperty("lastContactDateTimestamp")
    String lastContactDateTimestamp;
}
