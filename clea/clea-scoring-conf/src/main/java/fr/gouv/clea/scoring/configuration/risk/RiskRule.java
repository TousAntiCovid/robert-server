package fr.gouv.clea.scoring.configuration.risk;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

import fr.gouv.clea.scoring.configuration.ScoringRule;

@Valid
@SuperBuilder
@ToString(callSuper = true)
@Getter
public class RiskRule extends ScoringRule {
    @Positive
    private int clusterThresholdBackward;
    @Positive
    private int clusterThresholdForward;
    @Positive
    private float riskLevelBackward;
    @Positive
    private float riskLevelForward;

    public RiskRule(int venueType, int venueCategory1, int venueCategory2,
                    int clusterThresholdBackward, int clusterThresholdForward,
                    float riskLevelBackward, float riskLevelForward) {
        super(venueType, venueCategory1, venueCategory2);
        this.clusterThresholdBackward = clusterThresholdBackward;
        this.clusterThresholdForward = clusterThresholdForward;
        this.riskLevelBackward = riskLevelBackward;
        this.riskLevelForward = riskLevelForward;
    }

}
