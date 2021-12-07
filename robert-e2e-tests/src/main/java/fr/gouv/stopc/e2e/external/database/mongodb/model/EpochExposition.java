package fr.gouv.stopc.e2e.external.database.mongodb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * An epoch exposition reflects the exposure of the user (temporal and frequency
 * information).
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class EpochExposition {

    private int epochId;

    private List<Double> expositionScores;
}
