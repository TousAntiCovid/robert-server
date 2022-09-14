package fr.gouv.stopc.robertserver.crypto.test;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.model.ClientIdentifier;
import lombok.SneakyThrows;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Base64;

import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.cipherForStoredKey;

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

    public static byte[] findKeyForTuplesByIdA(final ByteString idA) {
        final var base64EncodedIdA = Base64.getEncoder().encodeToString(idA.toByteArray());
        return findKeyForTuplesByIdA(base64EncodedIdA);
    }

    @SneakyThrows
    public static byte[] findKeyForTuplesByIdA(final String idA) {
        final var base64Key = jdbcTemplate
                .queryForObject("select key_for_tuples from IDENTITY where idA = ?", String.class, idA);
        final var encryptedKey = Base64.getDecoder().decode(base64Key);
        return cipherForStoredKey().decrypt(encryptedKey);
    }

    public static int insert(final ClientIdentifier clientIdentifier) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("idA", clientIdentifier.getIdA());
        parameters.addValue("key_for_mac", clientIdentifier.getKeyForMac());
        parameters.addValue("key_for_tuples", clientIdentifier.getKeyForTuples());
        return new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("IDENTITY")
                .usingGeneratedKeyColumns("id")
                .execute(parameters);
    }

}
