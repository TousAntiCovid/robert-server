package fr.gouv.clea.utils;

import fr.gouv.clea.entity.ExposedVisit;

import java.util.Comparator;

/**
 * Compare by periodStart, if same, by timeSlot. *
 */
public class ExposedVisitComparator implements Comparator<ExposedVisit> {

	@Override
	public int compare(ExposedVisit v1, ExposedVisit v2) {
		if(null==v1) {
			return -1;
		}
		if(null==v2) {
			return 1;
		}
		int byPeriod= Long.compare(v1.getPeriodStart(),v2.getPeriodStart());
		if(0==byPeriod) {
			return Integer.compare(v1.getTimeSlot(),v2.getTimeSlot());
		}
		return byPeriod;
	}

}
