package fr.gouv.stopc.robertserver.crypto.test;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM;
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoHMACSHA256;
import lombok.SneakyThrows;
import org.assertj.core.api.ListAssert;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.cipherForStoredKey;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link TestExecutionListener} to start a PostgreSQL container to be used as
 * a dependency for SpringBootTests.
 * <p>
 * It starts a postgresql container statically and export required system
 * properties to override Spring application context configuration.
 */
public class PostgreSqlManager implements TestExecutionListener {

    private static final JdbcDatabaseContainer POSTGRE = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:9.6")
    );

    private static JdbcTemplate jdbcTemplate;

    static {
        POSTGRE.start();
        System.setProperty("spring.datasource.url", POSTGRE.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRE.getUsername());
        System.setProperty("spring.datasource.password", POSTGRE.getPassword());
    }

    public static ListAssert<Map<String, Object>> assertThatAllIdentities() {
        return assertThat(jdbcTemplate.queryForList("select * from identity"));
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        jdbcTemplate = testContext.getApplicationContext()
                .getBean(JdbcTemplate.class);
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        jdbcTemplate.execute("DROP SCHEMA public CASCADE ;");
        jdbcTemplate.execute("CREATE SCHEMA public ;");
        testContext.getApplicationContext()
                .getBean(Flyway.class)
                .migrate();
    }

    @SneakyThrows
    public static void givenIdentityExistsForIdA(String base64EncodedIdA) {
        final var keyForMac = new byte[32];
        final var keyForTuples = new byte[32];
        final var random = new SecureRandom();
        random.nextBytes(keyForMac);
        random.nextBytes(keyForTuples);
        final var encryptedKeyForMac = cipherForStoredKey().encrypt(keyForMac);
        final var encryptedKeyForTuples = cipherForStoredKey().encrypt(keyForTuples);
        jdbcTemplate.update(
                "insert into identity(id, idA, key_for_mac, key_for_tuples, creation_time, last_update)" +
                        " values ((select nextval('identity_id_seq ')), ?, ?, ?, now(), now())",
                base64EncodedIdA,
                Base64.getEncoder().encodeToString(encryptedKeyForMac),
                Base64.getEncoder().encodeToString(encryptedKeyForTuples)
        );
    }

    public static void givenIdentityDoesntExistForIdA(String base64EncodedIdA) {
        jdbcTemplate.update("delete from identity where idA = ?", base64EncodedIdA);
    }

    public static CryptoAESGCM getCipherForTuples(final ByteString idA) {
        final var base64EncodedIdA = Base64.getEncoder().encodeToString(idA.toByteArray());
        return findKeyForTuplesByIdA(base64EncodedIdA);
    }

    @SneakyThrows
    public static CryptoAESGCM findKeyForTuplesByIdA(final String base64EncodedIdA) {
        final var base64Key = jdbcTemplate
                .queryForObject("select key_for_tuples from IDENTITY where idA = ?", String.class, base64EncodedIdA);
        final var encryptedKey = Base64.getDecoder().decode(base64Key);
        final var key = cipherForStoredKey().decrypt(encryptedKey);
        return new CryptoAESGCM(key);
    }

    @SneakyThrows
    public static CryptoHMACSHA256 getCipherForMac(final String base64EncodedIdA) {
        final var base64Key = jdbcTemplate
                .queryForObject("select key_for_mac from identity where idA = ?", String.class, base64EncodedIdA);
        final var encryptedKey = Base64.getDecoder().decode(base64Key);
        final var keyForMac = cipherForStoredKey().decrypt(encryptedKey);
        return new CryptoHMACSHA256(keyForMac);
    }
}
