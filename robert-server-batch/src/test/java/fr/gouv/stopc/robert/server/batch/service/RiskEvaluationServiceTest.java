package fr.gouv.stopc.robert.server.batch.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager;
import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

import static fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager.*;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        GrpcMockManager.class,
        MetricsManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RiskEvaluationServiceTest {

    private final ContactService contactService;

    private final IRegistrationService registrationService;

    private final ContactProcessingService contactProcessingService;

    private final RiskEvaluationService riskEvaluationService;

    private TestContext testContext;

    private final String RISK_DETECTED = "Risk detected\\. Aggregated risk since [\\d]{0,}: [\\d]{0,}\\.[\\d]{0,} greater than threshold [\\d]{0,}\\.[\\d]{0,}";

    @BeforeEach
    public void before(@Autowired TestContext testContext) {
        this.testContext = testContext;
        givenCryptoServerEpochId(this.testContext.currentEpochId);
    }

    @AfterEach
    public void afterAll() {
        this.contactService.deleteAll();
        this.registrationService.deleteAll();
    }

    @Test
    void metricIsIncrementedWhenProcessPerformed() {
        riskEvaluationService.performs();
        assertThatTimerMetricIncrement("robert.batch", "operation", "REGISTRATION_RISK_EVALUATION_STEP").isEqualTo(1L);
    }

    @Test
    void testScoreAndProcessRisksWithABadEncryptedCountryCodeShouldNotUpdateRegistration() throws Exception {

        // Given
        var registration = this.testContext.acceptableRegistration();
        this.testContext.generateAcceptableContactForRegistration(registration);

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));
        // set bad country code
        givenCryptoServerCountryCode(ByteString.copyFrom(new byte[] { (byte) 0xff }));

        // When
        contactProcessingService.performs();
        riskEvaluationService.performs();

        // Then
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));

        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registration.getPermanentIdentifier());

        assertTrue(expectedRegistration.isPresent());

        assertTrue(CollectionUtils.isEmpty(expectedRegistration.get().getExposedEpochs()));
        assertFalse(expectedRegistration.get().isAtRisk());
    }

    @Test
    void testScoreAndProcessRiskskWhenScoresEqualsZeroldShouldNotBeAtRisk() {

        var registration = this.testContext.acceptableRegistrationWithExistingScoreEqualToZero();

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        // When
        riskEvaluationService.performs();

        // Then
        assertThat(this.contactService.findAll()).isEmpty();
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registration.getPermanentIdentifier());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().isAtRisk())
                .as("Registration risk")
                .isFalse();
    }

    @Test
    void testScoreAndProcessRisksWhenRecentExposedEpochScoreGreaterThanRiskThresholdShouldBeAtRisk() {

        // Given
        var registration = this.testContext.acceptableRegistrationWithExistingScoreAboveThreshold();

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        // When
        try (final var logCaptor = LogCaptor.forClass(BatchRegistrationServiceImpl.class)) {
            riskEvaluationService.performs();

            // Then
            assertThat(this.contactService.findAll()).isEmpty();
            Optional<Registration> expectedRegistration = this.registrationService
                    .findById(registration.getPermanentIdentifier());
            assertThat(expectedRegistration).isPresent();
            assertThat(expectedRegistration.get().isAtRisk())
                    .as("Registration risk")
                    .isTrue();
            assertThatLogsMatchingRegex(logCaptor.getInfoLogs(), RISK_DETECTED, 1);
        }
    }

    @Test
    void testScoreAndProcessRisksWhenEpochScoresLessThanRiskThresholdShouldNotBeAtRisk() {

        // Given
        var registration = this.testContext.acceptableRegistrationWithExistingScoreBelowThreshold();

        givenCryptoServerIdA(ByteString.copyFrom(registration.getPermanentIdentifier()));

        // When
        riskEvaluationService.performs();

        // Then
        assertThat(this.contactService.findAll()).isEmpty();
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registration.getPermanentIdentifier());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().isAtRisk())
                .as("Registration risk")
                .isFalse();
    }

    private void assertThatLogsMatchingRegex(List<String> logs, String regex, int times) {
        final Condition<String> rowMatchingRegex = new Condition<>(value -> value.matches(regex), regex);
        assertThat(logs).as("Number of logs matching the regular expression").haveExactly(times, rowMatchingRegex);
    }
}
