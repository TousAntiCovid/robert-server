package fr.gouv.stopc.robertserver.ws.test;

import com.mongodb.client.MongoCollection;
import fr.gouv.stopc.robertserver.database.model.Registration;
import lombok.SneakyThrows;
import org.assertj.core.api.ListAssert;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class MongodbManager implements TestExecutionListener {

    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
            DockerImageName.parse("mongo:4.2.11")
    );

    private static MongoOperations mongoOperations;

    static {
        MONGO_DB_CONTAINER.start();
        System.setProperty(
                "spring.data.mongodb.uri",
                MONGO_DB_CONTAINER.getReplicaSetUrl("robert") + "?connectTimeoutMS=1000&socketTimeoutMS=1000"
        );
    }

    @Override
    public void beforeTestMethod(@NonNull TestContext testContext) {
        givenMongodbIsOnline();
        mongoOperations = testContext.getApplicationContext().getBean(MongoOperations.class);
        mongoOperations.getCollectionNames()
                .stream()
                .map(mongoOperations::getCollection)
                .forEach(MongoCollection::drop);
    }

    public static void givenRegistrationExists(final Registration registration) {
        mongoOperations.save(registration);
    }

    /**
     * Ensure the container is not "paused", "unpausing" it if necessary.
     */
    private static void givenMongodbIsOnline() {
        final var docker = MONGO_DB_CONTAINER.getDockerClient();
        final var mongodbContainerInspect = docker.inspectContainerCmd(MONGO_DB_CONTAINER.getContainerId())
                .exec();
        if (TRUE.equals(mongodbContainerInspect.getState().getPaused())) {
            docker.unpauseContainerCmd(MONGO_DB_CONTAINER.getContainerId())
                    .exec();
        }
    }

    /**
     * Put the container in "paused" state. Can be used to simulate failure.
     */
    public static void givenMongodbIsOffline() {
        MONGO_DB_CONTAINER.getDockerClient()
                .pauseContainerCmd(MONGO_DB_CONTAINER.getContainerId())
                .exec();
    }

    @SneakyThrows
    private static void exec(String... args) {
        new ProcessBuilder().command(args)
                .start()
                .waitFor(5, SECONDS);
    }

    public static ListAssert<Registration> assertThatRegistrations() {
        return assertThat(mongoOperations.find(new Query(), Registration.class));
    }
}
