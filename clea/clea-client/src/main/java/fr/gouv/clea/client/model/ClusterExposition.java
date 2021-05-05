package fr.gouv.clea.client.model;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClusterExposition {
    @JsonProperty("s")
    private long startTimeAsNtpTimestamp;
    
    @JsonProperty("d")
    private int durationInSeconds;

    @JsonProperty("r")
    private float risk;

    public boolean isInExposition(Instant instant) throws IOException {
        Instant startTime = Instant.ofEpochSecond(startTimeAsNtpTimestamp - ScannedQrCode.SECONDS_FROM_01_01_1900_TO_01_01_1970);
        long delta = Duration.between(startTime, instant).toSeconds();
        return (delta >= 0 && delta <= durationInSeconds);

    }
}
