package fr.gouv.clea.indexation.model.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.clea.dto.SinglePlaceCluster;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ClusterFileItem {

    @JsonProperty("ltid")
    private String temporaryLocationId;

    @JsonProperty("exp")
    private List<ExposureRow> exposures;

    public static ClusterFileItem ofCluster(SinglePlaceCluster cluster) {
        List<ExposureRow> exposureRows = new ArrayList<>();
        cluster.getPeriods().forEach(periods -> exposureRows.add(ExposureRow.builder()
                .startTimestamp(periods.getClusterStart())
                .durationInSeconds(periods.getClusterDurationInSeconds())
                .riskLevel(periods.getRiskLevel())
                .build()));

        return ClusterFileItem.builder()
                .temporaryLocationId(cluster.getLocationTemporaryPublicId().toString())
                .exposures(exposureRows)
                .build();
    }
}
