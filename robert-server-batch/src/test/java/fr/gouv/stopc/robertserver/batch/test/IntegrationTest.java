package fr.gouv.stopc.robertserver.batch.test;

import fr.gouv.stopc.robert.server.batch.RobertServerBatchApplication;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@Retention(RUNTIME)
@Target(TYPE)
@ActiveProfiles({ "dev", "test" })
// FIXME: remove @DirtiesContext when dark springbatch config permits it
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = RobertServerBatchApplication.class)
@TestExecutionListeners(listeners = {
        GrpcMockManager.class,
        LogbackManager.class,
        MongodbManager.class,
        MessageMatcher.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public @interface IntegrationTest {

}
