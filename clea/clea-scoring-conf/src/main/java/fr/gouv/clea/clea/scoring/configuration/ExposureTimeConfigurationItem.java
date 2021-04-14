package fr.gouv.clea.clea.scoring.configuration;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
@Getter
public class ExposureTimeConfigurationItem extends ScoringConfigurationItem {
    private int exposureTimeBackward;
    private int exposureTimeForward;
    private int exposureTimeStaffBackward;
    private int exposureTimeStaffForward;

    @Builder
    public ExposureTimeConfigurationItem(int venueType, int venueCategory1, int venueCategory2,
            int exposureTimeBackward, int exposureTimeForward,
            int exposureTimeStaffBackward, int exposureTimeStaffForward) {
        super(venueType, venueCategory1, venueCategory2);
        this.exposureTimeBackward = exposureTimeBackward;
        this.exposureTimeForward = exposureTimeForward;
        this.exposureTimeStaffBackward = exposureTimeStaffBackward;
        this.exposureTimeStaffForward = exposureTimeStaffForward;
    }

}
