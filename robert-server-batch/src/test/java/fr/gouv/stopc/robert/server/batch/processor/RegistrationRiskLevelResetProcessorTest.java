package fr.gouv.stopc.robert.server.batch.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.stopc.robert.server.batch.service.BatchStatisticsService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RegistrationRiskLevelResetProcessorTest {

    @Mock
    private PropertyLoader propertyLoader;

    private RegistrationRiskLevelResetProcessor processor;

    private ListAppender<ILoggingEvent> processorLoggerAppender;

    @Mock
    private BatchStatisticsService batchStatisticsService;

    @BeforeEach
    public void beforeEach() {
        when(this.propertyLoader.getRiskLevelRetentionPeriodInDays()).thenReturn(7);
        final var robertClock = new RobertClock("2020-06-01");
        this.processor = new RegistrationRiskLevelResetProcessor(
                this.propertyLoader, robertClock, batchStatisticsService
        );
    }

    @Test
    public void testRegistrationShouldNotBeUpdatedWhenNotAtRiskAndNotNotified() throws Exception {
        // Given
        Registration registration = Registration.builder().atRisk(false).isNotified(false).build();
        // When
        Registration processedRegistration = this.processor.process(registration);
        // Then
        assertThat(processedRegistration).isNull();
    }

    @Test
    public void testRiskLevelShouldNotBeResetWhenNotAtRiskAndNotified() throws Exception {
        // Given
        Registration registration = Registration.builder().atRisk(false).isNotified(true).build();
        // When
        Registration processedRegistration = this.processor.process(registration);
        // Then
        assertThat(processedRegistration).isNull();
    }

    @Test
    public void risk_level_should_not_be_reset_when_at_risk_and_notified_but_last_contact_date_is_under_7_days_ago()
            throws Exception {

        // Given
        final long nowMinus6DaysNtpTimestamp = TimeUtils.convertUnixMillistoNtpSeconds(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(7, DAYS)
                        .toEpochMilli()
        );

        Registration registration = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(4912)
                .lastContactTimestamp(nowMinus6DaysNtpTimestamp)
                .build();

        // When
        Registration processedRegistration = this.processor.process(registration);
        // Then
        assertThat(processedRegistration).isNull();
    }

    @Test
    public void risk_level_should_be_reset_when_at_risk_and_notified_and_last_contact_date_is_above_7_days_ago()
            throws Exception {

        // Given
        final long nowMinus8DaysNtpTimestamp = TimeUtils.convertUnixMillistoNtpSeconds(
                Instant.now()
                        .truncatedTo(DAYS)
                        .minus(8, DAYS)
                        .toEpochMilli()
        );

        Registration registration = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(4808)
                .lastContactTimestamp(nowMinus8DaysNtpTimestamp)
                .build();

        // When
        Registration processedRegistration = this.processor.process(registration);
        // Then
        assertThat(processedRegistration).isNotNull();
        assertThat(processedRegistration.isAtRisk()).isFalse();
        assertThat(processedRegistration.isNotified()).isTrue();
    }

    @Test
    public void testRiskLevelShouldBeResetWhenAtRiskAndNotNotifiedAndEpochMinimunIsReached() throws Exception {
        // Given
        this.setUpLogHandler();
        Registration registration = Registration.builder().atRisk(true).isNotified(false).latestRiskEpoch(4808).build();
        // When
        Registration processedRegistration = this.processor.process(registration);
        // Then
        assertThat(processedRegistration).isNotNull();
        assertThat(processedRegistration.isAtRisk()).isFalse();
        assertThat(processedRegistration.isNotified()).isFalse();
        assertUserAtRiskButNotNotifiedIsLogged();

    }

    protected void assertUserAtRiskButNotNotifiedIsLogged() {
        assertThat(this.processorLoggerAppender.list.size()).isEqualTo(1);
        ILoggingEvent log = this.processorLoggerAppender.list.get(0);
        assertThat(log.getMessage()).contains("Resetting risk level of a user never notified!");
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
    }

    private void setUpLogHandler() {
        this.processorLoggerAppender = new ListAppender<>();
        this.processorLoggerAppender.start();
        ((Logger) LoggerFactory.getLogger(RegistrationRiskLevelResetProcessor.class))
                .addAppender(this.processorLoggerAppender);
    }
}
