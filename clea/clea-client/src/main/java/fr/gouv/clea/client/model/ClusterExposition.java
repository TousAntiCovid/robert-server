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
    private long startTime;
    
    @JsonProperty("d")
    private int duration;

    @JsonProperty("r")
    private float risk;

    public boolean isInExposition(long timestamp) throws IOException{
        return ((timestamp >= startTime) && (timestamp <= startTime + duration * CleaClientConfiguration.getInstance().getDurationUnit())); 
    }
}
