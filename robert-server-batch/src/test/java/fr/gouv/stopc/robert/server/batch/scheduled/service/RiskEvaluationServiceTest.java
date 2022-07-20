package fr.gouv.stopc.robert.server.batch.scheduled.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.util.CollectionUtils;

import java.util.Optional;

import static fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        GrpcMockManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// @TestPropertySource( properties = {
// "robert.protocol.risk-threshold=0.01"
// })
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RiskEvaluationServiceTest {

    private final ContactService contactService;

    // private final CryptoService cryptoService;
    // private final IServerConfigurationService serverConfigurationService;
    // private final PropertyLoader propertyLoader;
    private final IRegistrationService registrationService;

    private final ContactProcessingService contactProcessingService;

    private final RiskEvaluationService riskEvaluationService;

    // @MockBean
    // private ICryptoServerGrpcClient cryptoServerClient;
    //
    // @MockBean
    // private RobertServerBatchConfiguration config;
    //
    // private Optional<Registration> registration;
    // private byte[] serverKey;
    // private byte countryCode;
    // private Key federationKey;
    // private long epochDuration;
    // private long serviceTimeStart;
    private TestContext testContext;

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

    // TODO : test process et non risk evaluation
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

    // TODO : test tout pourris car si au dessus ou au dessous déjà testés
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
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(2); // TODO : useless
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
        riskEvaluationService.performs();

        // Then
        assertThat(this.contactService.findAll()).isEmpty();
        Optional<Registration> expectedRegistration = this.registrationService
                .findById(registration.getPermanentIdentifier());
        assertThat(expectedRegistration).isPresent();
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(2); // TODO : useless
        assertThat(expectedRegistration.get().isAtRisk())
                .as("Registration risk")
                .isTrue();

        // TODO : ajouter le test sur : "Risk detected. Aggregated risk since {}: {}
        // greater than threshold {}"
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
        assertThat(expectedRegistration.get().getExposedEpochs())
                .isNotEmpty()
                .hasSize(2); // TODO : useless
        assertThat(expectedRegistration.get().isAtRisk())
                .as("Registration risk")
                .isFalse();
    }
}
