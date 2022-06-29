package fr.gouv.stopc.robert.server.batch.service;

import fr.gouv.stopc.robert.server.batch.IntegrationTest;
import fr.gouv.stopc.robert.server.batch.scheduled.service.ReassessRiskLevelService;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.time.Instant;

import static fr.gouv.stopc.robert.server.batch.manager.MetricsManager.assertThatCounterMetricIncrement;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReassessRiskLevelServiceTest {

    private final ReassessRiskLevelService reassessRiskLevelService;

    private final RobertClock robertClock;

    private final RegistrationRepository registrationRepository;

    @Test
    void risk_level_should_not_be_reset_when_not_at_risk_and_not_notified() {
        // Given
        byte[] rndBytes = getRandomId();

        var registration = Registration.builder()
                .atRisk(false)
                .isNotified(false)
                .permanentIdentifier(rndBytes)
                .build();
        registrationRepository.save(registration);

        // When
        reassessRiskLevelService.process();

        // Then
        Registration updatedRegistration = registrationRepository.findById(rndBytes).orElse(null);

        assertThat(updatedRegistration).as("Registration is null").isNotNull();
        assertThat(updatedRegistration).as("Object has not been updated").isEqualTo(registration);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true").isEqualTo(0L);
    }

    @Test
    void risk_level_should_not_be_reset_when_not_at_risk_and_notified() {
        // Given
        byte[] rndBytes = getRandomId();

        var registration = Registration.builder()
                .atRisk(false)
                .isNotified(true)
                .permanentIdentifier(rndBytes)
                .build();
        registrationRepository.save(registration);

        // When
        reassessRiskLevelService.process();

        // Then
        Registration updatedRegistration = registrationRepository.findById(rndBytes).orElse(null);

        assertThat(updatedRegistration).as("Registration is null").isNotNull();
        assertThat(updatedRegistration).as("Object has not been updated").isEqualTo(registration);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true").isEqualTo(0L);
    }

    @Test
    void risk_level_should_not_be_reset_when_at_risk_and_notified_but_last_contact_date_is_under_7_days_ago() {

        // Given
        final var nowMinus6DaysNtpTimestamp = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(7, DAYS)
        ).asNtpTimestamp();

        byte[] rndBytes = getRandomId();

        var registration = Registration.builder()
                .atRisk(false)
                .isNotified(true)
                .permanentIdentifier(rndBytes)
                .latestRiskEpoch(4912)
                .lastContactTimestamp(nowMinus6DaysNtpTimestamp)
                .build();
        registrationRepository.save(registration);

        // When
        reassessRiskLevelService.process();

        // Then
        Registration updatedRegistration = registrationRepository.findById(rndBytes).orElse(null);
        assertThat(updatedRegistration).as("Registration is null").isNotNull();
        assertThat(updatedRegistration).as("Object has not been updated").isEqualTo(registration);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true").isEqualTo(0L);
    }

    @Test
    void risk_level_should_be_reset_when_at_risk_and_notified_and_last_contact_date_is_above_7_days_ago() {
        // Given
        byte[] rndBytes = getRandomId();

        final var nowMinus8DaysNtpTimestamp = robertClock.at(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(8, DAYS)
        ).asNtpTimestamp();

        registrationRepository.save(
                Registration.builder()
                        .permanentIdentifier(rndBytes)
                        .atRisk(true)
                        .isNotified(true)
                        .latestRiskEpoch(4808)
                        .lastContactTimestamp(nowMinus8DaysNtpTimestamp)
                        .build()
        );

        // When
        reassessRiskLevelService.process();

        // Then
        Registration processedRegistration = registrationRepository.findById(rndBytes).orElse(null);

        AssertionsForClassTypes.assertThat(processedRegistration).as("Registration is null").isNotNull();
        AssertionsForClassTypes.assertThat(processedRegistration.isAtRisk()).as("Registration is not at risk")
                .isFalse();
        AssertionsForClassTypes.assertThat(processedRegistration.isNotified())
                .as("Registration is notified for current risk").isTrue();
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true")
                .as("Increment between before test method and now").isEqualTo(1L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false").isEqualTo(0L);
    }

    @Test
    void risk_level_should_be_reset_when_at_risk_and_not_notified_and_epoch_minimum_is_reached() {
        // Given
        byte[] rndBytes = getRandomId();
        registrationRepository.save(
                Registration.builder()
                        .permanentIdentifier(rndBytes)
                        .atRisk(true)
                        .isNotified(false)
                        .latestRiskEpoch(4808)
                        .build()
        );

        // When
        reassessRiskLevelService.process();
        Registration processedRegistration = registrationRepository.findById(rndBytes).orElse(null);

        // Then
        assertThat(processedRegistration).as("Registration is null").isNotNull();
        assertThat(processedRegistration.isAtRisk()).as("Registration is not at risk").isFalse();
        assertThat(processedRegistration.isNotified()).as("Registration is not notified for current risk").isFalse();
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "false")
                .as("Increment between before test method and now").isEqualTo(1L);
        assertThatCounterMetricIncrement("robert.batch.risk.reset", "notified", "true")
                .as("Increment between before test method and now").isEqualTo(0L);
    }

    private byte[] getRandomId() {
        SecureRandom sr = new SecureRandom();
        byte[] rndBytes = new byte[5];
        sr.nextBytes(rndBytes);
        return rndBytes;
    }
}
