package fr.gouv.stopc.robertserver.crypto.test

import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

@Retention(RUNTIME)
@Target(CLASS)
@ActiveProfiles("test", "jks")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(
    listeners = [
        KeystoreManager::class,
        LogbackManager::class,
        PostgreSqlManager::class,
        RobertCryptoGrpcManager::class
    ],
    mergeMode = MERGE_WITH_DEFAULTS
)
@ContextConfiguration(
    initializers = [
        PostgreSqlManager::class,
        RobertCryptoGrpcManager::class
    ]
)
@DisplayNameGeneration(ReplaceUnderscores::class)
annotation class IntegrationTest
