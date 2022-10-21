package fr.gouv.stopc.robert.server.batch;

import fr.gouv.stopc.robertserver.batch.test.MongodbManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@ActiveProfiles("legacy")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(listeners = { MongodbManager.class }, mergeMode = MERGE_WITH_DEFAULTS)
@Retention(RUNTIME)
@DisplayNameGeneration(ReplaceUnderscores.class)
public @interface IntegrationLegacyTest {

}
