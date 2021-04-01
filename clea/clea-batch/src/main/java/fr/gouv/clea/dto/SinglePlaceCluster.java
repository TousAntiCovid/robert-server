package fr.gouv.clea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinglePlaceCluster {
	private UUID locationTemporaryPublicId;
	private int venueType;
	private int venueCategory1;
	private int venueCategory2;

	@Builder.Default
	private List<ClusterPeriod> periods = new ArrayList<>();

	public static SinglePlaceCluster initialise(SinglePlaceExposedVisits record) {
    	return SinglePlaceCluster.builder()
    		.locationTemporaryPublicId(record.getLocationTemporaryPublicId())
			.venueType(record.getVenueType())
			.venueCategory1(record.getVenueCategory1())
			.venueCategory2(record.getVenueCategory2())
			.build();
    }

	public void addPeriod(ClusterPeriod period) {
		periods.add(period);
	}

	/**
	 * @return true when periods is empty (the location has not enough visits to initialize a Period of cluster). 
	 */
	public boolean isEmpty() {
		return periods.isEmpty();
	}

	public void computeDurations(int unit) {
		periods.forEach(p->p.computeClusterStartAndDuration(unit));
	}
}
