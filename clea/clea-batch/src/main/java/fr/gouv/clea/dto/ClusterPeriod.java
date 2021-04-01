package fr.gouv.clea.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.clea.entity.ExposedVisit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClusterPeriod {

	//variable matching ExposedVisit.periodStart (NTP in hour)
	private long periodStart;

	private int firstTimeSlot;
	private int lastTimeSlot;
	
	private long clusterStart;
	
	private int clusterDurationInSeconds;
	
	private float riskLevel;
	
	public void adjustLimit(final ExposedVisit v) {
		lastTimeSlot=Math.max(lastTimeSlot, v.getTimeSlot());
	}

	public boolean isInSameCluster(final ExposedVisit v) {
		return periodStart == v.getPeriodStart() && firstTimeSlot < v.getTimeSlot() && v.getTimeSlot() <= lastTimeSlot + 1;
	}

	/**
	 * Compute clusterStart and clusterDuration once, when all contiguous slots
	 * are identified
	 * 
	 * @param unit
	 */
	public void computeClusterStartAndDuration(int unit) {
		clusterStart= periodStart + (firstTimeSlot*unit);
		clusterDurationInSeconds = (lastTimeSlot-firstTimeSlot+1) * unit;
	}


}
