package fr.gouv.stopc.robertserver.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * An epoch exposition reflects the exposure of the user (temporal and frequency information). 
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EpochExposition {
    private int epochId;

    private List<Double> expositionScores;
}
