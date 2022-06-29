package fr.gouv.stopc.robert.server.batch;

import fr.gouv.stopc.robert.server.batch.manager.MetricsManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@SpringBootTest
@TestExecutionListeners(listeners = {
        MetricsManager.class
}, mergeMode = MERGE_WITH_DEFAULTS)
@ActiveProfiles({ "legacy", "test" })
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface IntegrationTest {
}
