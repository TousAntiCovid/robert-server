package fr.gouv.stopc.e2e.mobileApplication.model;

import fr.gouv.stopc.robert.client.model.ExposureStatusResponse;
import lombok.Value;

import java.time.Instant;

import static fr.gouv.stopc.e2e.external.common.utils.TimeUtils.convertNTPSecondsToUnixMillis;
import static java.lang.Long.parseLong;
import static java.time.Instant.ofEpochMilli;

@Value
public class ExposureStatus {

    Instant lastContactDate;

    Integer riskLevel;

    public ExposureStatus(ExposureStatusResponse exposureStatusResponse) {
        this.riskLevel = exposureStatusResponse.getRiskLevel();
        Instant lastContactDate = null;
        if (null != exposureStatusResponse.getLastContactDate()) {
            lastContactDate = ofEpochMilli(
                    convertNTPSecondsToUnixMillis(parseLong(exposureStatusResponse.getLastContactDate()))
            );
        }
        this.lastContactDate = lastContactDate;
    }

}
