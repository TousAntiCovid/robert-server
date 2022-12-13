package fr.gouv.stopc.robertserver.crypto.test

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256
import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.base64Encode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import org.flywaydb.core.Flyway
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.random.Random

private lateinit var jdbcTemplate: JdbcTemplate

/**
 * A [TestExecutionListener] to start a PostgreSQL container to be used as a dependency for SpringBootTests.
 *
 * It starts a postgresql container and export required system properties to override Spring application context configuration.
 */
class PostgreSqlManager : TestExecutionListener, ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:13.8")).apply {
            start()
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext,
                "spring.datasource.url=$jdbcUrl",
                "spring.datasource.username=$username",
                "spring.datasource.password=$password"
            )
        }
    }

    override fun beforeTestClass(testContext: TestContext) {
        jdbcTemplate = testContext.applicationContext
            .getBean(JdbcTemplate::class.java)
    }

    override fun beforeTestMethod(testContext: TestContext) {
        jdbcTemplate.execute("DROP SCHEMA public CASCADE ;")
        jdbcTemplate.execute("CREATE SCHEMA public ;")
        testContext.applicationContext
            .getBean(Flyway::class.java)
            .migrate()
    }
}

/**
 * Verify existing application identities stored in database.
 */
fun assertThatAllIdentities(): ListAssert<Map<String, Any>> =
    assertThat(jdbcTemplate.queryForList("select * from identity"))

/**
 * Creates an `idA` in the database with two random keys for MAC and TUPLES computation.
 *
 * Generated keys can be retrieved using [getCipherForTuples] and [getCipherForMac].
 */
fun givenIdentityExistsForIdA(base64EncodedIdA: String) {
    val keyForMac = Random.nextBytes(32)
    val keyForTuples = Random.nextBytes(32)
    val encryptedKeyForMac = cipherForStoredKey().encrypt(keyForMac)
    val encryptedKeyForTuples = cipherForStoredKey().encrypt(keyForTuples)
    jdbcTemplate.update(
        """
        insert into identity(id, idA, key_for_mac, key_for_tuples, creation_time, last_update)
            values ((select nextval('identity_id_seq ')), ?, ?, ?, now(), now())
            
        """.trimIndent(),
        base64EncodedIdA,
        encryptedKeyForMac.base64Encode(),
        encryptedKeyForTuples.base64Encode()
    )
}

/**
 * Ensure an `idA` doesn't exist, delete it if necessary.
 */
fun givenIdentityDoesntExistForIdA(base64EncodedIdA: String) {
    jdbcTemplate.update("delete from identity where idA = ?", base64EncodedIdA)
}

/**
 * Returns the cipher to encrypt/decrypt TUPLES for the given `idA`.
 */
fun getCipherForTuples(idA: ByteString): CryptoAESGCM = getCipherForTuples(idA.toByteArray().base64Encode())

/**
 * Returns the cipher to encrypt/decrypt TUPLES for the given `idA`.
 */
fun getCipherForTuples(base64EncodedIdA: String): CryptoAESGCM {
    val base64Key = jdbcTemplate
        .queryForObject("select key_for_tuples from IDENTITY where idA = ?", String::class.java, base64EncodedIdA)
    val key = cipherForStoredKey().decrypt(base64Key.base64Decode())
    return CryptoAESGCM(key)
}

/**
 * Returns the cipher to encrypt/decrypt MACs for the given `idA`.
 */
fun getCipherForMac(base64EncodedIdA: String): CryptoHMACSHA256 {
    val base64Key = jdbcTemplate
        .queryForObject("select key_for_mac from identity where idA = ?", String::class.java, base64EncodedIdA)
    val keyForMac = cipherForStoredKey().decrypt(base64Key.base64Decode())
    return CryptoHMACSHA256(keyForMac)
}
