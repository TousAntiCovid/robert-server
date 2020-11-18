package test.fr.gouv.stopc.robertserver.batch.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.stopc.robert.server.batch.RobertServerBatchApplication;
import fr.gouv.stopc.robert.server.batch.configuration.RobertServerBatchConfiguration;
import fr.gouv.stopc.robert.server.batch.processor.RegistrationRiskProcessor;
import fr.gouv.stopc.robert.server.batch.service.ScoringStrategyService;
import fr.gouv.stopc.robert.server.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.INotificationService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.extern.slf4j.Slf4j;
import test.fr.gouv.stopc.robertserver.batch.utils.ProcessorTestUtils;

@Slf4j
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { RobertServerBatchApplication.class })
@TestPropertySource(locations = "classpath:application.properties", properties = "robert.scoring.algo-version=2")
public class RegistrationRiskProcessorTest {

    private final static String SHOULD_NOT_FAIL = "It should not fail";

    private RegistrationRiskProcessor riskProcessor;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @Autowired
    private ScoringStrategyService scoringStrategyService;

    @Autowired
    private IRegistrationService registrationService;

    @Autowired
    private PropertyLoader propertyLoader;

    @Autowired
    private INotificationService notificationService;

    @MockBean
    private RobertServerBatchConfiguration config;

    private Optional<Registration> registration;

    @BeforeEach
    public void before() {

        this.riskProcessor = new RegistrationRiskProcessor(
                this.notificationService,
                this.serverConfigurationService,
                this.scoringStrategyService,
                this.propertyLoader);
    }

    @Test
    public void testProcessingWhenRegistrationIsNullShouldFail() {

        try {
            // When
            Registration processedRegistration = this.riskProcessor.process(null);

            // Then
            assertNull(processedRegistration);
        } catch (Exception e) {
            log.error(e.getMessage());
            fail(SHOULD_NOT_FAIL);
        }
    }

    @Test
    public void testProcessingRegistrationWithoutExposedEpochShouldNotBeAtRisk() {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());

        try {
            // When
            Registration processedRegistration = this.riskProcessor.process(this.registration.get());

            // Then
            assertNotNull(processedRegistration);
            assertFalse(processedRegistration.isAtRisk());
        } catch (Exception e) {
            log.error(e.getMessage());
            fail(SHOULD_NOT_FAIL);
        }
    }

    @Test
    public void testProcessingRegistrationWithTooOlderExposedEpochShouldNotBeAtRisk() {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        
        
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        int val = (this.propertyLoader.getContagiousPeriod() * 24 * 3600)
                / this.serverConfigurationService.getEpochDurationSecs();
        val++;
        int tooOldEpochId = currentEpochId - val;
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition
                .builder()
                .epochId(tooOldEpochId)
                .expositionScores(Arrays.asList(0.0))
                .build()));
        try {
            // When
            Registration processedRegistration = this.riskProcessor.process(registrationWithEE);

            // Then
            assertNotNull(processedRegistration);
            assertTrue(CollectionUtils.isEmpty(processedRegistration.getExposedEpochs()));
            assertFalse(processedRegistration.isAtRisk());
        } catch (Exception e) {
            log.error(e.getMessage());
            fail(SHOULD_NOT_FAIL);
        }
    }

    @Test
    public void testProcessingRegistrationWithRecentAndTooOlderExposedEpochShouldNotBeAtRisk() {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        
        
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        int val = (this.propertyLoader.getContagiousPeriod() * 24 * 3600)
                / this.serverConfigurationService.getEpochDurationSecs();
        val++;
        int tooOldEpochId = currentEpochId - val;
        Registration registrationWithEE = this.registration.get();
        
        EpochExposition currentEpochExposition =  EpochExposition.builder()
                .epochId(currentEpochId)
                .expositionScores(Arrays.asList(0.01))
                .build();

        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition
                .builder()
                .epochId(tooOldEpochId)
                .expositionScores(Arrays.asList(0.0))
                .build(),
                currentEpochExposition));
        try {
            // When
            Registration processedRegistration = this.riskProcessor.process(registrationWithEE);

            // Then
            assertNotNull(processedRegistration);
            assertFalse(CollectionUtils.isEmpty(processedRegistration.getExposedEpochs()));
            assertTrue(processedRegistration.getExposedEpochs().contains(currentEpochExposition));
            assertFalse(processedRegistration.isAtRisk());
        } catch (Exception e) {
            log.error(e.getMessage());
            fail(SHOULD_NOT_FAIL);
        }
    }

    @Test
    public void testProcessingRegistrationWithRecentLesserThanRiskThresholdExposedEpochShouldNotBeAtRisk() {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        
        
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

        // Setup id with an existing score below threshold
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition.builder()
                .epochId(previousEpoch)
                .expositionScores(Arrays.asList(0.0))
                .build(),
                EpochExposition.builder()
                .epochId(currentEpochId)
                .expositionScores(Arrays.asList(0.0))
                .build()));

        try {
            // When
            Registration processedRegistration = this.riskProcessor.process(registrationWithEE);

            // Then
            assertNotNull(processedRegistration);
            assertFalse(CollectionUtils.isEmpty(processedRegistration.getExposedEpochs()));
            assertEquals(processedRegistration.getExposedEpochs().size(), registrationWithEE.getExposedEpochs().size());
            assertFalse(processedRegistration.isAtRisk());
        } catch (Exception e) {
            log.error(e.getMessage());
            fail(SHOULD_NOT_FAIL);
        }
    }
    

    @Test
    public void testProcessingRegistrationWithRecentExposedEpochGreaterThanRiskThresholdShouldNotBeAtRisk() {

        // Given
        this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
        assertTrue(this.registration.isPresent());
        
        
        final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
        final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

        final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);
        final int previousEpoch = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime - 900);

        // Setup id with an existing score below threshold
        Registration registrationWithEE = this.registration.get();
        registrationWithEE.setExposedEpochs(Arrays.asList(EpochExposition.builder()
                .epochId(previousEpoch)
                .expositionScores(Arrays.asList(14.0))
                .build(),
                EpochExposition.builder()
                .epochId(currentEpochId)
                .expositionScores(Arrays.asList(2.0))
                .build()));

        assertTrue(CollectionUtils.isEmpty(this.notificationService.findAll()));

        try {
            // When
            Registration processedRegistration = this.riskProcessor.process(registrationWithEE);

            // Then
            assertNotNull(processedRegistration);
            assertFalse(CollectionUtils.isEmpty(processedRegistration.getExposedEpochs()));
            assertEquals(processedRegistration.getExposedEpochs().size(), registrationWithEE.getExposedEpochs().size());
            assertFalse(CollectionUtils.isEmpty(this.notificationService.findAll()));
            assertTrue(processedRegistration.isAtRisk());
        } catch (Exception e) {
            log.error(e.getMessage());
            fail(SHOULD_NOT_FAIL);
        }
    }
}
