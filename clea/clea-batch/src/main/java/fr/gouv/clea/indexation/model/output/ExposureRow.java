package fr.gouv.clea.indexation.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExposureRow {

    @JsonProperty("s")
    private long startTimestamp;

    @JsonProperty("d")
    private long durationInSeconds;

    @JsonProperty("r")
    private float riskLevel;

}
