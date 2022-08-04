package fr.gouv.stopc.robert.server.batch;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager;
import fr.gouv.stopc.robert.server.batch.manager.MongodbManager;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.service.ContactService;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;

import static fr.gouv.stopc.robert.server.batch.manager.GrpcMockManager.givenCryptoServerIdA;
import static fr.gouv.stopc.robert.server.batch.manager.HelloMessageFactory.generateHelloMessagesStartingAndDuringSeconds;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.givenContactExistForUser;
import static fr.gouv.stopc.robert.server.batch.manager.MongodbManager.givenRegistrationExistsForUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@IntegrationTest
@TestExecutionListeners(listeners = {
        GrpcMockManager.class,
        MongodbManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "robert-batch.command-line-runner.reasses-risk.enabled=true"
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class RobertCommandLineRunnerTest {

    private final ContactService contactService;

    private final IRegistrationService registrationService;

    private final RobertCommandLineRunner robertBatch;

    private final RobertClock clock;

    @Test
    void can_log_how_many_hello_messages_will_be_processed() {
        // Given
        givenRegistrationExistsForUser("user___1");
        givenContactExistForUser(
                "user___1", c -> c.messageDetails(generateHelloMessagesStartingAndDuringSeconds(clock.now(), 90))
        );

        givenCryptoServerIdA(ByteString.copyFrom("user___1".getBytes()));

        try (final var logCaptor = LogCaptor.forClass(RobertCommandLineRunner.class)) {
            robertBatch.run("");

            assertThat(logCaptor.getInfoLogs())
                    .contains(
                            "9 hello messages waiting for process",
                            "0 hello messages remaining after process"
                    );
        }
    }

    @AfterEach
    public void afterAll() {
        this.contactService.deleteAll();
        this.registrationService.deleteAll();
    }

}
