package test.fr.gouv.stopc.robertserver.batch.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.server.batch.RobertServerBatchApplication;
import fr.gouv.stopc.robert.server.batch.configuration.ContactsProcessingConfiguration;
import fr.gouv.stopc.robert.server.batch.configuration.RobertServerBatchConfiguration;
import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.service.INotificationService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import test.fr.gouv.stopc.robertserver.batch.utils.ProcessorTestUtils;

@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {  AnalyticsNotificationProcessingConfigurationTest.BatchTestConfig.class, RobertServerBatchApplication.class })
@TestPropertySource(locations="classpath:application.properties", properties = {"robert.scoring.algo-version=2",
"robert.scoring.batch-mode=MIGRATE_RISK_NOTIFICATION"})
public class AnalyticsNotificationProcessingConfigurationTest {

    private final static String SHOULD_NOT_FAIL = "It should not fail";

    @Inject
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private IServerConfigurationService serverConfigurationService;

    @Autowired
    private IRegistrationService registrationService;

    @Autowired
    private INotificationService notificationService;

    @MockBean
    private ICryptoServerGrpcClient cryptoServerClient;

    @MockBean
    private RobertServerBatchConfiguration config;

    private Optional<Registration> registration;

    @Test
    public void testProcessNotNotifiedRegistrationShouldNotGenerateAnAnalyticsNotification()
    {
        try {
            // Given
            this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
            assertTrue(this.registration.isPresent());

            assertTrue(CollectionUtils.isEmpty(this.notificationService.findAll()));

            // When
            this.jobLauncherTestUtils.launchJob();

            // Then
            assertTrue(CollectionUtils.isEmpty(this.notificationService.findAll()));

        } catch (Exception e) {

            fail(SHOULD_NOT_FAIL);
        }

    }

    @Test
    public void testProcessNotNotifiedRegistrationShouldGenerateAnAnalyticsNotification()
    {
        try {
            // Given
            this.registration = this.registrationService.createRegistration(ProcessorTestUtils.generateIdA());
            assertTrue(this.registration.isPresent());

            final long tpstStart = this.serverConfigurationService.getServiceTimeStart();
            final long currentTime = TimeUtils.convertUnixMillistoNtpSeconds(new Date().getTime());

            final int currentEpochId = TimeUtils.getNumberOfEpochsBetween(tpstStart, currentTime);

            Registration notifiedRegistration = this.registration.get();
            notifiedRegistration.setNotified(true);
            notifiedRegistration.setLatestRiskEpoch(currentEpochId);

            this.registrationService.saveRegistration(notifiedRegistration);


            assertTrue(CollectionUtils.isEmpty(this.notificationService.findAll()));

            // When
            this.jobLauncherTestUtils.launchJob();

            // Then
            assertFalse(CollectionUtils.isEmpty(this.notificationService.findAll()));
            assertEquals(this.notificationService.findAll().size(), 1);

        } catch (Exception e) {

            fail(SHOULD_NOT_FAIL);
        }

    }

    @Configuration
    @Import({ ContactsProcessingConfiguration.class })
    public static class BatchTestConfig {

        private Job scoreAndProcessRisks;

        @Bean
        @Inject
        JobLauncherTestUtils jobLauncherTestUtils(Job scoreAndProcessRisks) {

            this.scoreAndProcessRisks = scoreAndProcessRisks;

            JobLauncherTestUtils jobLauncherTestUtils = new JobLauncherTestUtils();
            jobLauncherTestUtils.setJob(this.scoreAndProcessRisks);

            return jobLauncherTestUtils;
        }

    }

}
