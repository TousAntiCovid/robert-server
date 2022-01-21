package fr.gouv.stopc.robert.server.batch.processor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.properties")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class RegistrationRiskLevelResetProcessorTest {

    @Mock
    private PropertyLoader propertyLoader;

    @Mock
    private IServerConfigurationService serverConfigurationService;

    @InjectMocks
    private RegistrationRiskLevelResetProcessor processor;

    private ListAppender<ILoggingEvent> processorLoggerAppender;

    @BeforeEach
    public void beforeEach() {

        when(this.propertyLoader.getRiskLevelRetentionPeriodInDays()).thenReturn(7);
        long serviceTimeStart = Instant.now().getEpochSecond() + TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970
                - 5000L * TimeUtils.EPOCH_DURATION_SECS;
        when(this.serverConfigurationService.getServiceTimeStart()).thenReturn(serviceTimeStart);
        this.processor = new RegistrationRiskLevelResetProcessor(this.propertyLoader, this.serverConfigurationService);
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
        final long nowMinus6DaysEpoch = TimeUtils.convertUnixMillistoNtpSeconds(
                Instant.now().minus(6, ChronoUnit.DAYS).atZone(TimeZone.getDefault().toZoneId())
                        .toInstant().toEpochMilli()
        );

        Registration registration = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(4912)
                .lastContactTimestamp(nowMinus6DaysEpoch)
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
        final long nowMinus8DaysEpoch = TimeUtils.convertUnixMillistoNtpSeconds(
                Instant.now().minus(8, ChronoUnit.DAYS).atZone(TimeZone.getDefault().toZoneId())
                        .toInstant().toEpochMilli()
        );

        Registration registration = Registration.builder()
                .atRisk(true)
                .isNotified(true)
                .latestRiskEpoch(4808)
                .lastContactTimestamp(nowMinus8DaysEpoch)
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
        assertThat(log.getMessage()).contains(RegistrationRiskLevelResetProcessor.USER_AT_RISK_NOT_NOTIFIED_MESSAGE);
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
    }

    private void setUpLogHandler() {
        this.processorLoggerAppender = new ListAppender<>();
        this.processorLoggerAppender.start();
        ((Logger) LoggerFactory.getLogger(RegistrationRiskLevelResetProcessor.class))
                .addAppender(this.processorLoggerAppender);
    }
}
