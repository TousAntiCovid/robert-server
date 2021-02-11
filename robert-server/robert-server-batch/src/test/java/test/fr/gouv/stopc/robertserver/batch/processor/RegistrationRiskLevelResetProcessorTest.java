package test.fr.gouv.stopc.robertserver.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.stopc.robert.server.batch.processor.RegistrationRiskLevelResetProcessor;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.database.model.Registration;

@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application.properties")
public class RegistrationRiskLevelResetProcessorTest {
    @Mock
    private PropertyLoader propertyLoader;
    private RegistrationRiskLevelResetProcessor processor;
    private ListAppender<ILoggingEvent> processorLoggerAppender;

    @BeforeEach
    public void beforeEach() {
        when(this.propertyLoader.getRiskLevelRetentionPeriodInDays()).thenReturn(2);
        this.processor = new RegistrationRiskLevelResetProcessor(this.propertyLoader);
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
    public void testRiskLevelShouldNotBeResetWhenAtRiskAndNotifiedButEpochMinimunIsNotReached() throws Exception {
        // Given
        Registration registration = Registration.builder().atRisk(true).isNotified(true).lastStatusRequestEpoch(5000)
                .latestRiskEpoch(4912).build();
        // When
        Registration processedRegistration = this.processor.process(registration);
        // Then
        assertThat(processedRegistration).isNull();
    }

    @Test
    public void testRiskLevelShouldBeResetWhenAtRiskAndNotifiedAndEpochMinimunIsReached() throws Exception {
        // Given
        Registration registration = Registration.builder().atRisk(true).isNotified(true).lastStatusRequestEpoch(5000)
                .latestRiskEpoch(4808).build();
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
        Registration registration = Registration.builder().atRisk(true).isNotified(false).lastStatusRequestEpoch(5000)
                .latestRiskEpoch(4808).build();
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
        ((Logger) LoggerFactory.getLogger(RegistrationRiskLevelResetProcessor.class)).addAppender(this.processorLoggerAppender);
    }
}
