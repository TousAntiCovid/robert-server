package fr.gouv.clea.indexation.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClusterFileIndex {

    @JsonProperty("i")
    private int iteration;

    @JsonProperty("c")
    private Set<String> prefixes;
}
