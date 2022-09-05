package fr.gouv.stopc.robert.crypto.grpc.server.test;

import fr.gouv.stopc.robert.crypto.grpc.server.storage.database.model.ClientIdentifier;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * A {@link TestExecutionListener} to start a PostgreSQL container to be used as
 * a dependency for SpringBootTests.
 * <p>
 * It starts a postgresql container statically and export required system
 * properties to override Spring application context configuration.
 */
public class PostgreSqlManager implements TestExecutionListener {

    private static final JdbcDatabaseContainer POSTGRE = new PostgreSQLContainer(DockerImageName.parse("postgres:9.6"));

    private static final ClientIdentifierRowMapper clientIdentifierRowMapper = new ClientIdentifierRowMapper();

    private static NamedParameterJdbcTemplate jdbcTemplate;

    static {
        POSTGRE.start();
        System.setProperty("spring.datasource.url", POSTGRE.getJdbcUrl());
        System.setProperty("spring.datasource.username", POSTGRE.getUsername());
        System.setProperty("spring.datasource.password", POSTGRE.getPassword());
    }

    @Override
    public void beforeTestClass(final TestContext testContext) {
        jdbcTemplate = testContext.getApplicationContext().getBean(NamedParameterJdbcTemplate.class);
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        jdbcTemplate.getJdbcTemplate().execute("DROP SCHEMA public CASCADE ;");
        jdbcTemplate.getJdbcTemplate().execute("CREATE SCHEMA public ;");
        testContext.getApplicationContext().getBean(Flyway.class).migrate();
    }

    public static ClientIdentifier findClientById(final String id) {
        final var parameter = Map.of("idA", id);
        return jdbcTemplate
                .queryForObject("select * from IDENTITY where idA = :idA", parameter, clientIdentifierRowMapper);
    }

}
