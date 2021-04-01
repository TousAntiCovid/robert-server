package fr.gouv.clea.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fr.gouv.clea.entity.ExposedVisit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinglePlaceExposedVisits {

    private UUID locationTemporaryPublicId;
    private int venueType;
    private int venueCategory1;
    private int venueCategory2;

    @Builder.Default
    private final List<ExposedVisit> visits= new ArrayList<>();

	public void addVisit(ExposedVisit v) {
		if(null!=v) {
			visits.add(v);
		}		
	}
}
