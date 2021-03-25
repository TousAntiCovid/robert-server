package fr.gouv.clea.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cluster {
    @JsonProperty("ltid")
    private String locationTemporaryPublicID; //LTid

    @JsonProperty("exp")
    private List<ClusterExposition> expositions;
}
