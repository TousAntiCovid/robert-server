package fr.gouv.clea.utils;

import fr.gouv.clea.entity.ExposedVisit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExposedVisitComparatorTest {

	
	
	@Test
	public void same() {
		ExposedVisitComparator c=new ExposedVisitComparator();
		ExposedVisit v1 = ExposedVisit.builder().periodStart(123).timeSlot(0).build();
		assertThat(c.compare(v1, v1)).isEqualTo(0);
	}

	@Test
	public void firstNull() {
		ExposedVisitComparator c=new ExposedVisitComparator();
		ExposedVisit v2 = ExposedVisit.builder().periodStart(123).timeSlot(0).build();
		assertThat(c.compare(null, v2)).isLessThan(0);
	}

	@Test
	public void secondNull() {
		ExposedVisitComparator c=new ExposedVisitComparator();
		ExposedVisit v1 = ExposedVisit.builder().periodStart(123).timeSlot(0).build();
		assertThat(c.compare(v1, null)).isGreaterThan(0);
	}
	
	@Test
	public void samePeriodNull() {
		ExposedVisitComparator c=new ExposedVisitComparator();

		ExposedVisit v1 = ExposedVisit.builder().periodStart(123).timeSlot(1).build();
		ExposedVisit v2 = ExposedVisit.builder().periodStart(123).timeSlot(2).build();
	
		assertThat(c.compare(v1, v2)).isLessThan(0);

		assertThat(c.compare(v2, v1)).isGreaterThan(0);

	}
	
	/**
	 * Sort depend on period and not Slot
	 */
	@Test
	public void differentPeriod() {
		ExposedVisitComparator c=new ExposedVisitComparator();

		ExposedVisit v1 = ExposedVisit.builder().periodStart(123).timeSlot(5).build();
		ExposedVisit v2 = ExposedVisit.builder().periodStart(456).timeSlot(1).build();
	
		assertThat(c.compare(v1, v2)).isLessThan(0);

		assertThat(c.compare(v2, v1)).isGreaterThan(0);

	}
}
