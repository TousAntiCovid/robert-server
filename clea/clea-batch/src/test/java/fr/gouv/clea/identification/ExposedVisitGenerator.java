package fr.gouv.clea.identification;

import fr.gouv.clea.entity.ExposedVisit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@SpringBootTest
//@ExtendWith(SpringExtension.class)
//@RunWith(SpringRunner.class)
//@ContextConfiguration(locationsForMirgation = {"/context/flywayContainerContext.xml" })
//@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
//                         FlywayTestExecutionListener.class })
//@DataJpaTest
@ActiveProfiles("jpatest")
@Slf4j
public class ExposedVisitGenerator {

	@Autowired
	DataSource ds;
//
//	@Autowired
//	protected Flyway flyway;
//
//	@BeforeAll
//	public void init() {
//		//flyway.configure().dataSource(ds);
////	    /flyway.clean();
////	    flyway.migrate();
//	}

	@Test
	@Disabled("for local development purpose")
	public void fillRandomVisits() {
		// hour of now : 3826008000
		// hour of 21-01-01 : 3818448000
		// diff: 7560000

		final int NB_LOCATIONS = 5000;
		final int batchSize = 10;

		final Random r = new Random();

		final long janv21 = 3818448000l;

		JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
		
		log.info("Starting to fill EXPOSED_VISITS...");
		
		for (int l = 0; l < NB_LOCATIONS; l++) {
			UUID lieu = UUID.randomUUID();
			int venueType = r.nextInt(18)+1; 		// 1 to 18
			int venueCategory1 = r.nextInt(4) + 1;  // 1 to 4
			int venueCategory2 = r.nextInt(4) + 1;  // 1 to 4

			List<ExposedVisit> batch = new ArrayList<>();

			long clusterStart = janv21 + (r.nextInt(65) * 1000l);
			int visitsPerPlace = r.nextInt(80);
			for (int slot = 0; slot <= visitsPerPlace; slot++) {

				//@formatter:off
				ExposedVisit v=ExposedVisit.builder()
						.locationTemporaryPublicId(lieu)
						.venueType(venueType)
						.venueCategory1(venueCategory1)
						.venueCategory2(venueCategory2)
						.periodStart(clusterStart)
						.timeSlot(slot)
						.forwardVisits(r.nextInt(2) * r.nextInt(100)) //  50%=0 or 50%= (1 to 10)
						.backwardVisits(r.nextInt(2) * r.nextInt(100))
						.build();
				//@formatter:on

				batch.add(v);
			}
			//@formatter:off
			jdbcTemplate.batchUpdate(
					"insert into EXPOSED_VISITS (LTId, venue_type, venue_category1, venue_category2, period_start, timeslot,backward_Visits, forward_Visits)"+
												"values (?,?,?,?,?,?,?,?)",
					batch,
					batchSize,
					new ParameterizedPreparedStatementSetter<ExposedVisit>() {
						@Override
						public void setValues(PreparedStatement ps, ExposedVisit visit)
						        throws SQLException {
							ps.setObject(1,visit.getLocationTemporaryPublicId());
							ps.setObject(2,visit.getVenueType());
							ps.setObject(3,visit.getVenueCategory1());
							ps.setObject(4,visit.getVenueCategory2());
							ps.setObject(5,visit.getPeriodStart());
							ps.setObject(6,visit.getTimeSlot());
							ps.setObject(7,visit.getBackwardVisits());
							ps.setObject(8,visit.getForwardVisits());
						}
					});
			//@formatter:on
			
		}
		log.info("Nb records in EXPOSED_VISITS: " + jdbcTemplate.queryForObject("select count(*) from EXPOSED_VISITS", Integer.class));

	}

}
