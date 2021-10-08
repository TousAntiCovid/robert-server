package fr.gouv.stopc.robert.integrationtest.utils;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("dev")
@TestPropertySource("classpath:application-dev.yml")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Retention(RUNTIME)
@Target(TYPE)
public @interface IntegrationTest {
}
