package fr.gouv.stopc.robert.server.batch;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("legacy")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Retention(RUNTIME)
@DisplayNameGeneration(ReplaceUnderscores.class)
public @interface IntegrationLegacyTest {
}
