package fr.gouv.clea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinglePlaceClusterPeriod {

    private UUID locationTemporaryPublicId;

    private int venueType;

    private int venueCategory1;

    private int venueCategory2;

    private long periodStart;

    private int firstTimeSlot;

    private int lastTimeSlot;

    private long clusterStart;

    private int clusterDurationInSeconds;

    private float riskLevel;
}
