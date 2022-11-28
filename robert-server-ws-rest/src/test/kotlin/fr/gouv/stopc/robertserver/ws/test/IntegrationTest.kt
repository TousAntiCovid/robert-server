package fr.gouv.stopc.robertserver.ws.test

import fr.gouv.stopc.robertserver.test.LogbackManager
import fr.gouv.stopc.robertserver.test.MongodbManager
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS

@Retention(RUNTIME)
@Target(ANNOTATION_CLASS, CLASS)
@ActiveProfiles("dev", "test")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(
    listeners = [AuthDataManager::class, JwtKeysManager::class, LogbackManager::class, RestAssuredManager::class, MongodbManager::class, MockServerManager::class, GrpcMockManager::class, StatisticsManager::class],
    mergeMode = MERGE_WITH_DEFAULTS
)
@DisplayNameGeneration(ReplaceUnderscores::class)
annotation class IntegrationTest
