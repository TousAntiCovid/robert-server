package fr.gouv.tacw.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.database.model.ScoreResult;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;

@DataJpaTest
@Transactional
class ExposedStaticVisitRepositoryTests {
    @Autowired
    private ExposedStaticVisitRepository exposedStaticVisitRepository;

    @MockBean
    private TacWarningDatabaseConfiguration configuration;

    private int lastVisitId = 0;
    private long referenceTime;
    private long retentionStart;
    int startDelta = 0;
    int endDelta = 2000;

    @BeforeEach
    void init() {
        assertThat(exposedStaticVisitRepository).isNotNull();
        assertThat(configuration).isNotNull();
        when(configuration.getVisitTokenRetentionPeriodDays())
                .thenReturn(8L);
        referenceTime  = System.currentTimeMillis();
        retentionStart = referenceTime - TimeUnit.DAYS.toSeconds(configuration.getVisitTokenRetentionPeriodDays());
    }

    /**
     *  |----------| <- exposed
     *               |----------| <- risk ?
     */
    @Test
    void testRiskScoreWhenPresenceTimeDoNotOverlapThenRiskIsZero() {
        ExposedStaticVisitEntity exposedStaticVisit;
        exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
        exposedStaticVisitRepository.save(exposedStaticVisit);
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 2001, retentionStart);

        assertScoreAndLastContactDate(scores, 0, -1);
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
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), exposedStaticVisit.getVisitStartTime(), retentionStart);

        assertScoreAndLastContactDate(scores, 1, referenceTime + 2000);
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
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 1000, retentionStart);

        assertScoreAndLastContactDate(scores, 1, referenceTime + 2000);
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
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime - 1000, retentionStart);

        assertScoreAndLastContactDate(scores, 1, referenceTime + 2000);
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
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime - 2000, retentionStart);

        assertScoreAndLastContactDate(scores, 0, -1);
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
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 2000, retentionStart);

        assertScoreAndLastContactDate(scores, 0, -1);
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
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 1000, retentionStart);

        assertScoreAndLastContactDate(scores, 2, referenceTime + 2000);
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
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 1600, retentionStart);

        assertScoreAndLastContactDate(scores, 3, referenceTime + 4800);
    }

    /**
     * In practice, this use case is not used because a riskScore is only computed for one visit,
     * and so one venue risk level.
     *
     *  |----------| <- exposed (risk high)
     *                   |----------| <- exposed (risk low)
     *    |----------| <- exposed (risk low)
     *                             |----------| <- exposed (risk high)
     *           |----------| <- risk ? should give risk low => 2, risk high => 1
     */
    @Test
    void testRiskScoreWhenManyExposedVisitsOverlappingWithMoreThanOneRiskLevelThenGetAsManyScoreResultAsRiskLevelsFoundInVisits() {
        ExposedStaticVisitEntity exposedStaticVisit;
        exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
        exposedStaticVisitRepository.save(exposedStaticVisit);
        exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), RiskLevel.LOW, referenceTime + 2800, referenceTime + 4800) );
        exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), RiskLevel.LOW, referenceTime + 400, referenceTime + 2400) );
        exposedStaticVisitRepository.save( this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), RiskLevel.HIGH, referenceTime + 4600, referenceTime + 6600) );
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), referenceTime + 1600, retentionStart);

        assertThat(scores.size()).isEqualTo(2);
        Optional<ScoreResult> lowRiskLevelScore = scores.stream()
            .filter(score -> score.getRiskLevel() == RiskLevel.LOW)
            .findFirst();
        assertThat(lowRiskLevelScore.isPresent());
        assertThat(lowRiskLevelScore.get().getScore()).isEqualTo(2);
        assertThat(lowRiskLevelScore.get().getLastContactDate()).isEqualTo(referenceTime + 4800);

        Optional<ScoreResult> highRiskLevelScore = scores.stream()
                .filter(score -> score.getRiskLevel() == RiskLevel.HIGH)
                .findFirst();
            assertThat(highRiskLevelScore.isPresent());
            assertThat(highRiskLevelScore.get().getScore()).isEqualTo(1);
            assertThat(highRiskLevelScore.get().getLastContactDate()).isEqualTo(referenceTime + 2000);
    }

    @Test
    void testRiskScoreWhenDifferentTokenThenRiskIsZero() {
        ExposedStaticVisitEntity exposedStaticVisit;
        exposedStaticVisit = this.newExposedStaticVisitEntity(referenceTime, referenceTime + 2000);
        exposedStaticVisitRepository.save(exposedStaticVisit);
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart - 1));

        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(DatatypeConverter.parseHexBinary(this.nextVisitId()), referenceTime, retentionStart);

        assertScoreAndLastContactDate(scores, 0, -1);
    }

    @Test
    void testRiskScoreWhenNoExposedTokenThenRiskIsZero() {
        // expired visit
        exposedStaticVisitRepository.save(this.newExposedStaticVisitEntity(retentionStart - 2, retentionStart - 1));
        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(DatatypeConverter.parseHexBinary(this.nextVisitId()), referenceTime, retentionStart);

        assertScoreAndLastContactDate(scores, 0, -1);
    }

    @Test
    void testRiskScoreWhenVisitEndsBeforeRetentionDate() {
        ExposedStaticVisitEntity exposedStaticVisit = this.newExposedStaticVisitEntity(retentionStart - 2, retentionStart - 1);
        exposedStaticVisitRepository.save(exposedStaticVisit);
        List<ScoreResult> scores = exposedStaticVisitRepository.riskScore(exposedStaticVisit.getToken(), retentionStart - 2, retentionStart);
        assertScoreAndLastContactDate(scores, 0, -1);
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

    private ExposedStaticVisitEntity newExposedStaticVisitEntity(byte[] token, long startTime, long endTime) {
        return this.newExposedStaticVisitEntity(token, RiskLevel.HIGH, startTime, endTime);
    }

    private ExposedStaticVisitEntity newExposedStaticVisitEntity(byte[] token, RiskLevel venueRiskLevel, long startTime, long endTime) {
        return new ExposedStaticVisitEntity(
                token,
                venueRiskLevel,
                startTime,
                endTime,
                startDelta,
                endDelta,
                1L);
    }

    private String nextVisitId() {
        this.lastVisitId++;
        return "ac831a7dd6cbe40751ac8d434bfa65cf8ae804133691283bdf2b3b113aea000" + Integer.toString(this.lastVisitId);
    }

    private void assertScoreAndLastContactDate(List<ScoreResult> scores, long score, long expectedLastContactDate) {
        if (score == 0) {
            assertThat(scores).isEmpty();
            return;
        }

        assertThat(scores.size()).isEqualTo(1);
        assertThat(scores.get(0).getScore()).isEqualTo(score);
        assertThat(scores.get(0).getLastContactDate()).isEqualTo(expectedLastContactDate);
    }
}
