package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.IntegrationLegacyTest;
import fr.gouv.stopc.robert.server.batch.configuration.PropertyLoader;
import fr.gouv.stopc.robert.server.batch.configuration.RobertServerBatchConfiguration;
import fr.gouv.stopc.robert.server.batch.service.BatchRegistrationService;
import fr.gouv.stopc.robert.server.batch.utils.ProcessorTestUtils;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ExtendWith(SpringExtension.class)
@IntegrationLegacyTest
@TestPropertySource(properties = {
        "robert.scoring.algo-version=2",
        "robert.scoring.batch-mode=FULL_REGISTRATION_SCAN_COMPUTE_RISK",
        "robert.server.time-start=2021-01-01"
})
class RiskEvaluationProcessorTest {

    private RiskEvaluationProcessor riskEvaluationProcessor;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @Autowired
    private IRegistrationService registrationService;

    @Autowired
    private BatchRegistrationService batchRegistrationService;

    @Autowired
    private PropertyLoader propertyLoader;

    private Optional<Registration> registration;

    @MockBean
    private RobertServerBatchConfiguration configuration;

    private int currentEpoch, lastExpositionEpoch;

    private int arbitraryScoreEpochStart = 500;

    private long expectedLastContactDate;

    private long lastExpositionTimestamp;

    private long expectedLastContactDateOffet;

    @BeforeEach
    public void beforeEach() {
        this.riskEvaluationProcessor = new RiskEvaluationProcessor(
                serverConfigurationService,
                propertyLoader,
                batchRegistrationService
        );
        this.currentEpoch = TimeUtils.getCurrentEpochFrom(serverConfigurationService.getServiceTimeStart());

        arbitraryScoreEpochStart = this.currentEpoch - (14 * TimeUtils.EPOCHS_PER_DAY) + 1
                + new SecureRandom().nextInt(100);
        lastExpositionEpoch = arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 7;
        lastExpositionTimestamp = TimeUtils.dayTruncatedTimestamp(lastExpositionEpoch);
        expectedLastContactDate = TimeUtils.dayTruncatedTimestamp(
                TimeUtils.getNtpSeconds(this.lastExpositionEpoch, serverConfigurationService.getServiceTimeStart())
        );
        expectedLastContactDateOffet = 24 * 3600; // +/- 1 day in seconds
    }

    @Test
    void testShouldReturnNullIfProvidedRegistrationIsNull() {
        assertThat(riskEvaluationProcessor.process(null)).isNull();
    }

    @Test
    void testWhenNoScoreThenNoRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertThat(this.registration).isPresent();

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatNoRiskDetected(returnedRegistration);
    }

    @Test
    void testWhenScoresNotAtRiskThenNoRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        ArrayList<EpochExposition> expositions = this.expositions(new Double[] { 1.0 }, new Double[] { 12.5 });
        this.registration.get().setExposedEpochs(expositions);
        this.registration.get().setOutdatedRisk(true);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatNoRiskDetected(returnedRegistration);
        assertThat(this.registration.get().isAtRisk()).isFalse();
        assertThat(this.registration.get().getLastContactTimestamp()).isZero();
        assertThat(this.registration.get().getLatestRiskEpoch()).isZero();
        assertThatRegistrationHasExactExpositions(this.registration.get(), expositions);
    }

    @Test
    void testWhenScoresAtRiskThenRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        ArrayList<EpochExposition> expositions = this.expositions(new Double[] { 1.0 }, new Double[] { 14.5 });
        this.registration.get().setExposedEpochs(expositions);
        this.registration.get().setOutdatedRisk(true);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration).isNotNull();
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp())
                .isCloseTo(expectedLastContactDate, Offset.offset(expectedLastContactDateOffet));
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }

    @Test
    void testWhenManyExpositionsGivingScoreAtRiskThenGetLastAtRiskExpositionDate() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.arbitraryScoreEpochStart)
                        .expositionScores(Arrays.asList(new Double[] { 1.0 }))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 3)
                        .expositionScores(Arrays.asList(new Double[] { 3.0 }))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.lastExpositionEpoch)
                        .expositionScores(Arrays.asList(new Double[] { 2.5 }))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 2)
                        .expositionScores(Arrays.asList(new Double[] { 7.0 }))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 6)
                        .expositionScores(Arrays.asList(new Double[] { 4.0 }))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 5)
                        .expositionScores(Arrays.asList(new Double[] { 2.5 }))
                        .build()
        );
        this.registration.get().setExposedEpochs(expositions);
        this.registration.get().setOutdatedRisk(true);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp()).isGreaterThan(lastExpositionTimestamp);
        assertThat(returnedRegistration.getLastContactTimestamp())
                .isCloseTo(expectedLastContactDate, Offset.offset(expectedLastContactDateOffet));
        log.info(
                "Last contact date: {}",
                Instant.ofEpochSecond(
                        returnedRegistration.getLastContactTimestamp() - TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970
                )
        );
    }

    @Test
    void testWhenScoresAtRiskForASingleEpochThenRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        ArrayList<EpochExposition> expositions = expositionsAtRisk();
        this.registration.get().setExposedEpochs(expositions);
        this.registration.get().setOutdatedRisk(true);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp())
                .isCloseTo(expectedLastContactDate, Offset.offset(expectedLastContactDateOffet));
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }

    @Test
    void testWhenNotifiedIsTrueAndRiskDetectedThenNotifiedRemainsTrue() {
        ArrayList<EpochExposition> expositions = this.expositionsAtRisk();
        this.setNewRegistration(true, expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.isNotified()).isTrue();
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp())
                .isCloseTo(expectedLastContactDate, Offset.offset(expectedLastContactDateOffet));
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }

    @Test
    void testWhenNotifiedIsTrueAndRiskNotDetectedThenNotifiedRemainsTrue() {
        ArrayList<EpochExposition> expositions = this.expositionsNotAtRisk();
        this.setNewRegistration(true, expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatNoRiskDetected(returnedRegistration);
        assertThat(this.registration.get().isAtRisk()).isFalse();
        assertThat(this.registration.get().isNotified()).isTrue();
        assertThat(this.registration.get().getLatestRiskEpoch()).isZero();
        assertThat(this.registration.get().getLastContactTimestamp()).isZero();
        assertThatRegistrationHasExactExpositions(this.registration.get(), expositions);
    }

    @Test
    void testWhenNotifiedIsFalseAndRiskDetectedThenNotifiedRemainsFalse() {
        ArrayList<EpochExposition> expositions = this.expositionsAtRisk();
        this.setNewRegistration(false, expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.isNotified()).isFalse();
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp())
                .isCloseTo(expectedLastContactDate, Offset.offset(expectedLastContactDateOffet));
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }

    @Test
    void testWhenNotifiedIsFalseAndRiskNotDetectedThenNotifiedRemainsFalse() {
        ArrayList<EpochExposition> expositions = this.expositionsNotAtRisk();
        this.setNewRegistration(false, expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatNoRiskDetected(returnedRegistration);
        assertThat(this.registration.get().isAtRisk()).isFalse();
        assertThat(this.registration.get().getLatestRiskEpoch()).isEqualTo(0);
        assertThat(this.registration.get().getLastContactTimestamp()).isEqualTo(0);
        assertThatRegistrationHasExactExpositions(this.registration.get(), expositions);
        assertThat(this.registration.get().isNotified()).isFalse();
    }

    @Test
    void testWhenAlreadyAtRiskAndNewContactAtRiskWithDateGreaterThanCurrentLastContactDateThenLastContactDateIsUpdated() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        this.registration.get().setAtRisk(true);
        this.registration.get().setLastContactTimestamp(this.expectedLastContactDate - (TimeUtils.EPOCHS_PER_DAY * 3));
        this.registration.get().setExposedEpochs(this.expositionsAtRisk());
        this.registration.get().setOutdatedRisk(true);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        assertThat(returnedRegistration.isAtRisk()).isTrue();
        assertThat(returnedRegistration.getLastContactTimestamp())
                .isCloseTo(expectedLastContactDate, Offset.offset(expectedLastContactDateOffet));
    }

    @Test
    void testWhenAlreadyAtRiskAndNewContactAtRiskWithDateLessThanCurrentLastContactDateThenLastContactDateIsNotUpdated() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        this.registration.get().setAtRisk(true);
        this.registration.get().setLastContactTimestamp(
                this.expectedLastContactDate + TimeUtils.EPOCHS_PER_DAY * TimeUtils.EPOCH_DURATION_SECS
        );
        this.registration.get().setExposedEpochs(this.expositionsAtRisk());
        this.registration.get().setOutdatedRisk(true);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        assertThat(returnedRegistration.isAtRisk()).isTrue();
        assertThat(returnedRegistration.getLastContactTimestamp())
                .isEqualTo(this.expectedLastContactDate + TimeUtils.EPOCHS_PER_DAY * TimeUtils.EPOCH_DURATION_SECS);
    }

    @Test
    void testShouldNotReturnALastContactDateInTheFutureWhenAtRisk() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        int exposedEpoch = this.currentEpoch + TimeUtils.EPOCHS_PER_DAY * 4;
        expositions.add(
                EpochExposition.builder()
                        .epochId(exposedEpoch)
                        .expositionScores(Arrays.asList(new Double[] { 10.0, 5.0 }))
                        .build()
        );
        this.registration.get().setExposedEpochs(expositions);
        this.registration.get().setOutdatedRisk(true);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp()).isEqualTo(
                TimeUtils.dayTruncatedTimestamp(
                        Instant.now().getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970
                )
        );
    }

    protected ArrayList<EpochExposition> expositionsAtRisk() {
        return this.expositions(new Double[] { 10.0, 2.0, 1.0, 4.3 }, new Double[] {});
    }

    protected ArrayList<EpochExposition> expositionsNotAtRisk() {
        return this.expositions(new Double[] { 10.0 }, new Double[] {});
    }

    protected ArrayList<EpochExposition> expositions(Double[] scores1, Double[] scores2) {
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.arbitraryScoreEpochStart)
                        .expositionScores(Arrays.asList(scores1))
                        .build()
        );
        expositions.add(
                EpochExposition.builder()
                        .epochId(this.lastExpositionEpoch)
                        .expositionScores(Arrays.asList(scores2))
                        .build()
        );
        return expositions;
    }

    protected void assertThatRiskDetected(Registration returnedRegistration) {
        assertThat(returnedRegistration).isNotNull();
        assertThat(returnedRegistration.isAtRisk()).isTrue();
    }

    protected void assertThatNoRiskDetected(Registration returnedRegistration) {
        assertThat(returnedRegistration).isNotNull();
        assertThat(returnedRegistration.isAtRisk()).isFalse();
    }

    protected void assertThatRegistrationHasExactExpositions(Registration returnedRegistration,
            List<EpochExposition> expectedExpositions) {
        IntStream.range(0, expectedExpositions.size())
                .forEach(idx -> {
                    assertThat(returnedRegistration.getExposedEpochs().get(idx).getExpositionScores())
                            .containsExactlyElementsOf(expectedExpositions.get(idx).getExpositionScores());
                });
    }

    protected void setNewRegistration(boolean isNotified, List<EpochExposition> expositions) {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        this.registration.get().setNotified(isNotified);
        this.registration.get().setExposedEpochs(expositions);
        this.registration.get().setOutdatedRisk(true);
    }
}
