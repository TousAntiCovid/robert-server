package fr.gouv.stopc.robert.crypto.grpc.server.test;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@Retention(RUNTIME)
@Target(TYPE)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(listeners = { PostgreSqlManager.class, KeystoreManager.class,
        DataManager.class }, mergeMode = MERGE_WITH_DEFAULTS)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public @interface IntegrationTest {

}
