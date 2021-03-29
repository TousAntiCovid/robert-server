package fr.gouv.clea.client.model;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.clea.client.configuration.CleaClientConfiguration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClusterExposition {
    @JsonProperty("s")
    private long startTimeAsNtpTimestamp;
    
    @JsonProperty("d")
    private int nbDurationUnit;

    @JsonProperty("r")
    private float risk;

    public boolean isInExposition(long ntpTimestamp) throws IOException {
        return ((ntpTimestamp >= startTimeAsNtpTimestamp) 
                && (ntpTimestamp <= startTimeAsNtpTimestamp + nbDurationUnit * CleaClientConfiguration.getInstance().getDurationUnitInSeconds())); 
    }
}
