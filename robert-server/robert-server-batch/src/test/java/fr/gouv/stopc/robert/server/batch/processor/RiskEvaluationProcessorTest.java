package fr.gouv.stopc.robert.server.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.stopc.robert.server.batch.RobertServerBatchApplication;
import fr.gouv.stopc.robert.server.batch.configuration.RobertServerBatchConfiguration;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.batch.utils.ProcessorTestUtils;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RobertServerBatchApplication.class })
@TestPropertySource(locations = "classpath:application.properties",
        properties = {
                "robert.scoring.algo-version=2",
                "robert.scoring.batch-mode=FULL_REGISTRATION_SCAN_COMPUTE_RISK"
        })
public class RiskEvaluationProcessorTest {

    private RiskEvaluationProcessor riskEvaluationProcessor;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @Autowired
    private IRegistrationService registrationService;

    @Autowired
    private BatchRegistrationServiceImpl batchRegistrationService;

    @Autowired
    private PropertyLoader propertyLoader;

    private Optional<Registration> registration;

    @MockBean
    private RobertServerBatchConfiguration configuration;

    private int currentEpoch, lastExpositionEpoch;
    private int arbitraryScoreEpochStart = 500;
    private long expectedLastContactDate;

    @BeforeEach
    public void beforeEach() {
        this.riskEvaluationProcessor = new RiskEvaluationProcessor(
                serverConfigurationService,
                propertyLoader,
                batchRegistrationService);
        this.currentEpoch = TimeUtils.getCurrentEpochFrom(this.serverConfigurationService.getServiceTimeStart());

        arbitraryScoreEpochStart = this.currentEpoch - (14 * 96) + new SecureRandom().nextInt(100) + 1;
        lastExpositionEpoch = arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 7;
        expectedLastContactDate = TimeUtils.dayTruncatedTimestamp(TimeUtils.getNtpSeconds(this.lastExpositionEpoch, serverConfigurationService.getServiceTimeStart()));
    }

    @Test
    public void testShouldReturnNullIfProvidedRegistrationIsNull() {
        assertThat(riskEvaluationProcessor.process(null)).isNull();
    }

    @Test
    public void testWhenNoScoreThenNoRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertThat(this.registration).isPresent();
        
        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatNoRiskDetected(returnedRegistration);
    }

    @Test
    public void testWhenScoresNotAtRiskThenNoRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        ArrayList<EpochExposition> expositions = this.expositions(new Double[] { 1.0 }, new Double[] { 12.5 });
        this.registration.get().setExposedEpochs(expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatNoRiskDetected(returnedRegistration);
        assertThat(this.registration.get().isAtRisk()).isFalse();
        assertThat(this.registration.get().getLastContactTimestamp()).isEqualTo(0);
        assertThat(this.registration.get().getLatestRiskEpoch()).isEqualTo(0);
        assertThatRegistrationHasExactExpositions(this.registration.get(), expositions);
    }

    @Test
    public void testWhenScoresAtRiskThenRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        ArrayList<EpochExposition> expositions = this.expositions(new Double[] { 1.0 }, new Double[] { 14.5 });
        this.registration.get().setExposedEpochs(expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp()).isEqualTo(expectedLastContactDate);
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }
    
    @Test
    public void testWhenManyExpositionsGivinfScoreAtRiskThenGetLastAtRiskExpositionDate() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(EpochExposition.builder()
                .epochId(this.arbitraryScoreEpochStart)
                .expositionScores(Arrays.asList(new Double[] { 1.0 }))
                .build());
        expositions.add(EpochExposition.builder()
                .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 3)
                .expositionScores(Arrays.asList(new Double[] { 3.0 }))
                .build());
        expositions.add(EpochExposition.builder()
                .epochId(this.lastExpositionEpoch)
                .expositionScores(Arrays.asList(new Double[] { 2.5 }))
                .build());
        expositions.add(EpochExposition.builder()
                .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 2)
                .expositionScores(Arrays.asList(new Double[] { 7.0 }))
                .build());
        expositions.add(EpochExposition.builder()
                .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 6)
                .expositionScores(Arrays.asList(new Double[] { 4.0 }))
                .build());
        expositions.add(EpochExposition.builder()
                .epochId(this.arbitraryScoreEpochStart + TimeUtils.EPOCHS_PER_DAY * 5)
                .expositionScores(Arrays.asList(new Double[] { 2.5 }))
                .build());
        this.registration.get().setExposedEpochs(expositions);
        
        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp()).isEqualTo(expectedLastContactDate);
    }

    @Test
    public void testWhenScoresAtRiskForASingleEpochThenRiskDetected() {
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        ArrayList<EpochExposition> expositions = expositionsAtRisk();
        this.registration.get().setExposedEpochs(expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());

        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp()).isEqualTo(expectedLastContactDate);
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }

    @Test
    public void testWhenNotifiedIsTrueAndRiskDetectedThenNotifiedRemainsTrue() {
        ArrayList<EpochExposition> expositions = this.expositionsAtRisk();
        this.setNewRegistration(true, expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());
        
        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.isNotified()).isTrue();
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp()).isEqualTo(expectedLastContactDate);
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }

    @Test
    public void testWhenNotifiedIsTrueAndRiskNotDetectedThenNotifiedRemainsTrue() {
        ArrayList<EpochExposition> expositions = this.expositionsNotAtRisk();
        this.setNewRegistration(true, expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());
        
        this.assertThatNoRiskDetected(returnedRegistration);
        assertThat(this.registration.get().isAtRisk()).isFalse();
        assertThat(this.registration.get().isNotified()).isTrue();
        assertThat(this.registration.get().getLatestRiskEpoch()).isEqualTo(0);
        assertThat(this.registration.get().getLastContactTimestamp()).isEqualTo(0);
        assertThatRegistrationHasExactExpositions(this.registration.get(), expositions);
    }

    @Test
    public void testWhenNotifiedIsFalseAndRiskDetectedThenNotifiedRemainsFalse() {
        ArrayList<EpochExposition> expositions = this.expositionsAtRisk();
        this.setNewRegistration(false, expositions);

        Registration returnedRegistration = this.riskEvaluationProcessor.process(this.registration.get());
        
        this.assertThatRiskDetected(returnedRegistration);
        assertThat(returnedRegistration.isNotified()).isFalse();
        assertThat(returnedRegistration.getLatestRiskEpoch()).isEqualTo(this.currentEpoch);
        assertThat(returnedRegistration.getLastContactTimestamp()).isEqualTo(expectedLastContactDate);
        assertThatRegistrationHasExactExpositions(returnedRegistration, expositions);
    }

    @Test
    public void testWhenNotifiedIsFalseAndRiskNotDetectedThenNotifiedRemainsFalse() {
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

    protected ArrayList<EpochExposition> expositionsAtRisk() {
        return this.expositions(new Double[] { 10.0, 2.0, 1.0, 4.3 }, new Double[] { });
    }

    protected ArrayList<EpochExposition> expositionsNotAtRisk() {
        return this.expositions(new Double[] { 10.0 }, new Double[] { });
    }

    protected ArrayList<EpochExposition> expositions(Double[] scores1, Double[] scores2) {
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        expositions.add(EpochExposition.builder()
                .epochId(this.arbitraryScoreEpochStart)
                .expositionScores(Arrays.asList(scores1))
                .build());
        expositions.add(EpochExposition.builder()
                .epochId(this.lastExpositionEpoch)
                .expositionScores(Arrays.asList(scores2))
                .build());
        return expositions;
    }
    
    protected void assertThatRiskDetected(Registration returnedRegistration) {
        assertThat(returnedRegistration).isNotNull();
        assertThat(returnedRegistration.isAtRisk()).isTrue();
    }
    
    protected void assertThatNoRiskDetected(Registration returnedRegistration) {
        assertThat(returnedRegistration).isNull();
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
    }
}
