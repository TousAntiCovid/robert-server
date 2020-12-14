package fr.gouv.tacw.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Transactional;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;

@DataJpaTest
@Transactional
class ExposedStaticVisitRepositoryTests {
	@Autowired
	ExposedStaticVisitRepository exposedStaticVisitRepository;
	
	private int lastVisitId = 0;
	private final long referenceTime = System.currentTimeMillis();
	int startDelta = 0;
	int endDelta = 2000;
	
	/**
	 *  |----------| <- exposed
	 *               |----------| <- risk ? 
	 */
	@Test
	void testRiskScoreWhenPresenceTimeDoNotOverlapThenRiskIsZero() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		
		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 2001);
		
		assertThat(0).isEqualTo(score);
	}
	
	/**
	 *  |----------| <- exposed
	 *  |----------| <- risk ?
	 */
	@Test
	void testRiskScoreWhenSamePresenceTimeThenRiskIsOne() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		
		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), exposedStaticVisit.getVisitStartTime());
		
		assertThat(1).isEqualTo(score);
	}
	
	/**
	 *  |----------| <- exposed
	 *         |----------| <- risk ?
	 */
	@Test
	void testRiskScoreWhenStartTimeOverlapThenRiskIsOne() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		
		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 1000);
		
		assertThat(1).isEqualTo(score);
	}
	
	/**
	 *        |----------| <- exposed
	 *  |----------| <- risk ?
	 */
	@Test
	void testRiskScoreWhenEndTimeOverlapThenRiskIsOne() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		
		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime - 1000);
		
		assertThat(1).isEqualTo(score);
	}
	
	/**
	 *             |----------| <- exposed
	 *  |----------| <- risk ?
	 */
	@Test
	void testRiskScoreWhenEndTimeIsEqualToExposedStartTimeThenRiskIsZero() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		
		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime - 2000);
		
		assertThat(0).isEqualTo(score);
	}
	
	
	/**
	 *  |----------| <- exposed
	 *             |----------| <- risk ?
	 */
	@Test
	void testRiskScoreWhenStartTimeIsEqualToExposedEndTimeThenRiskIsZero() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		
		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 2000);
		
		assertThat(0).isEqualTo(score);
	}
	
	/**
	 *  |----------| <- exposed
	 *  |----------| <- exposed
	 *        |----------| <- risk ?
	 */
	@Test
	void testRiskScoreWhenManyExposedVisitsOverlappingThenRiskScoreIsIncreasedManyTimes() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), referenceTime, referenceTime + 2000) );

		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 1000);
		
		assertThat(2).isEqualTo(score);
	}

	/**
	 *  |----------| <- exposed
	 *                   |----------| <- exposed
	 *    |----------| <- exposed
	 *                             |----------| <- exposed
	 *                   |----------| <- exposed, different token (venue)
	 *           |----------| <- risk ?
	 */
	@Test
	void testRiskScoreWhenManyExposedVisitsOverlappingThenRiskScoreIsIncreasedOnlyWithOverlappingVisits() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), referenceTime + 2800, referenceTime + 4800) );
		exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), referenceTime + 400, referenceTime + 2400) );
		exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), referenceTime + 4600, referenceTime + 6600) );
		exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(referenceTime + 4600, referenceTime + 6600) );

		long score = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 1600);
		
		assertThat(3).isEqualTo(score);
	}
	
	@Test
	void testRiskScoreWhenDifferentTokenThenRiskIsZero() {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);
		
		long score = exposedStaticVisitRepository.riskScore(DatatypeConverter.parseHexBinary(this.nextVisitId()), referenceTime);
		
		assertThat(0).isEqualTo(score);
	}

	@Test
	void testRiskScoreWhenNoExposedTokenThenRiskIsZero() {	
		long score = exposedStaticVisitRepository.riskScore(DatatypeConverter.parseHexBinary(this.nextVisitId()), referenceTime);
		
		assertThat(0).isEqualTo(score);
	}
	
	@Test
	void testCanFindAVisitEntityPreviouslyRegistered( ) {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);

		Optional<ExposedStaticVisitEntity> found = exposedStaticVisitRepository.findByTokenAndStartEnd(exposedStaticVisit.getToken(), exposedStaticVisit.getVisitStartTime(), exposedStaticVisit.getVisitEndTime());
		
		assertThat(found.isPresent()).isTrue();
		assertThat(found.get().getToken()).isEqualTo(exposedStaticVisit.getToken());
		assertThat(found.get().getVisitStartTime()).isEqualTo(exposedStaticVisit.getVisitStartTime());
		assertThat(found.get().getVisitEndTime()).isEqualTo(exposedStaticVisit.getVisitEndTime());
	}
	
	@Test
	void testWhenFindingAVisitEntityWithDifferentStartTimeThenGetNotFound( ) {
		ExposedStaticVisitEntity exposedStaticVisit;
		exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
		exposedStaticVisitRepository.save(exposedStaticVisit);

		Optional<ExposedStaticVisitEntity> found = exposedStaticVisitRepository.findByTokenAndStartEnd(exposedStaticVisit.getToken(), exposedStaticVisit.getVisitStartTime()+1, exposedStaticVisit.getVisitEndTime());
		
		assertThat(found.isPresent()).isFalse();
	}
	
	private ExposedStaticVisitEntity newExposedStaticVisitEntity(long startTime, long endTime) {
		return this.newExposedStaticVisitEntity(DatatypeConverter.parseHexBinary(this.nextVisitId()), startTime, endTime);
	}
	
	private ExposedStaticVisitEntity newExposedStaticVisitEntity(byte[] visit, long startTime, long endTime) {
		return new ExposedStaticVisitEntity(
				visit,
				startTime, 
				startTime + 2000,
				startDelta,
				endDelta,
				1L);
	}

	private String nextVisitId() {
		this.lastVisitId++;
		return "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea000" + Integer.toString(this.lastVisitId);
	}
}
