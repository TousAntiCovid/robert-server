package fr.gouv.stopc.e2e.mobileapplication.timemachine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * An epoch exposition reflects the exposure of the user (temporal and frequency
 * information).
 */
@Data
@Builder
public class EpochExposition {

    private int epochId;

    private List<Double> expositionScores;
}
