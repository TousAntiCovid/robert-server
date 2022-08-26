package fr.gouv.stopc.robert.server.batch.service;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager;
import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import fr.gouv.stopc.robert.server.batch.manager.MongodbManager;
import fr.gouv.stopc.robert.server.batch.service.impl.BatchRegistrationServiceImpl;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager.givenCryptoServerCountryCode;
import static fr.gouv.stopc.robert.server.batch.manager.HelloMessageFactory.generateHelloMessagesStartingAndDuring;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatLogsMatchingRegex;
import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatTimerMetricIncrement;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        GrpcMockManager.class,
        MongodbManager.class,
        MetricsManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RiskEvaluationServiceTest {

    private final ContactService contactService;

    private final IRegistrationService registrationService;

    private final ContactProcessingService contactProcessingService;

    private final RiskEvaluationService riskEvaluationService;

    private final RobertClock clock;

    @AfterEach
    public void afterAll() {
        this.contactService.deleteAll();
        this.registrationService.deleteAll();
    }

    @Test
    void metric_is_incremented_when_process_performed() {
        riskEvaluationService.performs();
        assertThatTimerMetricIncrement("robert.batch", "operation", "REGISTRATION_RISK_EVALUATION_STEP").isEqualTo(1L);
    }

    @Test
    void score_and_process_risks_with_a_bad_encrypted_country_code_should_not_update_registration() {
        var now = clock.now();

        // Given
        givenRegistrationExistsForUser("user___1");
        givenContactExistForUser(
                "user___1", now, c -> c.messageDetails(
                        generateHelloMessagesStartingAndDuring(
                                now,
                                Duration.of(120, ChronoUnit.SECONDS)
                        )
                )
        );

        // Set bad country code
        givenCryptoServerCountryCode(ByteString.copyFrom(new byte[] { (byte) 0xff }));

        // When
        contactProcessingService.performs();
        riskEvaluationService.performs();

        // Then
        assertTrue(CollectionUtils.isEmpty(this.contactService.findAll()));

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::getExposedEpochs)
                .asList()
                .isEmpty();
    }

    @Test
    void score_and_process_risk_when_scores_equals_zero_should_not_be_at_risk() {
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);
        var fiveDaysAgo = clock.now().minus(5, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(0.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(fiveDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(0.0))
                                                .build()
                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        riskEvaluationService.performs();

        // Then
        assertThatContactsToProcess().isEmpty();

        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();
    }

    @Test
    void score_and_process_risks_when_recent_exposed_epoch_score_greater_than_risk_threshold_should_be_at_risk() {

        // Given Registration With Existing Score Above Threshold
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(20.0))
                                                .build()

                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        try (final var logCaptor = LogCaptor.forClass(BatchRegistrationServiceImpl.class)) {
            riskEvaluationService.performs();

            // Then
            assertThatContactsToProcess().isEmpty();

            assertThatRegistrationForUser("user___1")
                    .extracting(Registration::isAtRisk)
                    .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                    .as("Registration risk")
                    .isTrue();

            final var RISK_DETECTED = "Risk detected\\. Aggregated risk since [\\d]{0,}: [\\d]{0,}\\.[\\d]{0,} greater than threshold [\\d]{0,}\\.[\\d]{0,}";
            assertThatLogsMatchingRegex(logCaptor.getInfoLogs(), RISK_DETECTED, 1);
        }
    }

    @Test
    void score_and_process_risks_when_epoch_scores_less_than_risk_threshold_should_not_be_at_risk() {
        // Given Registration With Existing Score Below Threshold
        var twoDaysAgo = clock.now().minus(2, ChronoUnit.DAYS);

        givenRegistrationExistsForUser(
                "user___1", r -> r
                        .exposedEpochs(
                                List.of(
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(3.0))
                                                .build(),
                                        EpochExposition.builder()
                                                .epochId(twoDaysAgo.asEpochId())
                                                .expositionScores(Collections.singletonList(4.3))
                                                .build()

                                )
                        )
                        .outdatedRisk(true)
        );

        // When
        riskEvaluationService.performs();

        // Then
        assertThatContactsToProcess().isEmpty();
        assertThatRegistrationForUser("user___1")
                .extracting(Registration::isAtRisk)
                .asInstanceOf(InstanceOfAssertFactories.BOOLEAN)
                .as("Registration risk")
                .isFalse();
    }

}
