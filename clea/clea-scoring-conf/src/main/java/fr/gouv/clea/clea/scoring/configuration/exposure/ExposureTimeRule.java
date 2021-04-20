package fr.gouv.clea.clea.scoring.configuration.exposure;

import fr.gouv.clea.clea.scoring.configuration.ScoringRule;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

@Valid
@SuperBuilder
@ToString(callSuper = true)
@Getter
public class ExposureTimeRule extends ScoringRule {
    @Positive
    private int exposureTimeBackward;
    @Positive
    private int exposureTimeForward;
    @Positive
    private int exposureTimeStaffBackward;
    @Positive
    private int exposureTimeStaffForward;

    public ExposureTimeRule(int venueType, int venueCategory1, int venueCategory2,
                            int exposureTimeBackward, int exposureTimeForward,
                            int exposureTimeStaffBackward, int exposureTimeStaffForward) {
        super(venueType, venueCategory1, venueCategory2);
        this.exposureTimeBackward = exposureTimeBackward;
        this.exposureTimeForward = exposureTimeForward;
        this.exposureTimeStaffBackward = exposureTimeStaffBackward;
        this.exposureTimeStaffForward = exposureTimeStaffForward;
    }

}
