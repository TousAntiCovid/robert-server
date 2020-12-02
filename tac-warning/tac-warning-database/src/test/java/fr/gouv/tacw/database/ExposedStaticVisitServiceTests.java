package fr.gouv.tacw.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.database.utils.TimeUtils;

//@ContextConfiguration(classes = { ExposedStaticVisitServiceImpl.class })
//@DataJpaTest
@SpringBootTest
@Transactional
class ExposedStaticVisitServiceTests {
	@Autowired
	ExposedStaticVisitService exposedStaticVisitService;

	@Autowired
	ExposedStaticVisitRepository exposedStaticVisitRepository;
	
	@Value("${tacw.database.visit_token_retention_period_days}")
	private long retentionDays;
	
	@Test
	void testCanRegisterNewExposedVisit() {
		final List<String> tokens = Stream.of(
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0001",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0002")
				.collect(Collectors.toList());
		assertThat(exposedStaticVisitRepository.count()).isEqualTo(0);
		
		exposedStaticVisitService.registerOrIncrementExposedStaticVisits( this.entitiesFrom(tokens, 2000) );

		assertThat(exposedStaticVisitRepository.count()).isEqualTo(2);
	}

	@Test
	void testCanUpdateExistingExposedVisit() {
		String token = "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0001";
		List<ExposedStaticVisitEntity> existingExposedVisits = Collections.singletonList(this.entityFrom(token, 2000));
		exposedStaticVisitService.registerOrIncrementExposedStaticVisits(existingExposedVisits);
		assertThat(exposedStaticVisitRepository.count()).isEqualTo(1);
		ExposedStaticVisitEntity exposedVisitToUpdate = this.entityFrom(token, 2000);

		exposedStaticVisitService.registerOrIncrementExposedStaticVisits( Collections.singletonList(exposedVisitToUpdate) );		
		assertThat(exposedStaticVisitRepository.count()).isEqualTo(1);
		Optional<ExposedStaticVisitEntity> updatedToken = exposedStaticVisitRepository.findByToken(token);
		assertThat(updatedToken.isPresent()).isTrue();
		assertThat(updatedToken.get().getExposureCount()).isEqualTo(2);
	}

	@Test
	void testDeleteExpiredTokens() {
		final List<String> expired_tokens = Stream.of(
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0001",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0002",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0003",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0004",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea0005")
				.collect(Collectors.toList());
		final List<String> valid_tokens = Stream.of(
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1001",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1002",
				"ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea1003")
				.collect(Collectors.toList());
		final long currentNtpTime = TimeUtils.convertUnixMillistoNtpSeconds(System.currentTimeMillis());
		final long windowStart = currentNtpTime - (retentionDays*86400);
		
		exposedStaticVisitService.registerOrIncrementExposedStaticVisits(this.entitiesFrom(expired_tokens, windowStart - 2150));
		exposedStaticVisitService.registerOrIncrementExposedStaticVisits(this.entitiesFrom(valid_tokens, windowStart + 500));
		
		final long nbDeletedTokens = exposedStaticVisitService.deleteExpiredTokens();
		
		assertThat(nbDeletedTokens).isEqualTo(expired_tokens.size());
		for(String token: expired_tokens) {
			assertThat(exposedStaticVisitRepository.findByToken(token).isPresent()).isFalse();
		}
		for(String token: valid_tokens) {
			assertThat(exposedStaticVisitRepository.findByToken(token)).isPresent();
		}
	}
	
	protected List<ExposedStaticVisitEntity> entitiesFrom(List<String> tokens, long visitTime) {
		return tokens.stream()
			.map( token -> this.entityFrom(token, visitTime))
			.collect(Collectors.toList());
	}

	protected ExposedStaticVisitEntity entityFrom(String token, long visitTime) {
		int startDelta = 0;
		int endDelta = 2000;
		long visitStartTime = visitTime - startDelta;
		long visitEndTime = visitTime + endDelta;
		long exposureCount = 1;
		return new ExposedStaticVisitEntity(token, visitStartTime, visitEndTime, startDelta, endDelta, exposureCount);
	}
}
