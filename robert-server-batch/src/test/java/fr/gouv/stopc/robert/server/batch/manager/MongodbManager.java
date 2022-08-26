package fr.gouv.stopc.robert.server.batch.manager;

import com.mongodb.client.MongoCollection;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.Contact.ContactBuilder;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.Registration.RegistrationBuilder;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Function;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
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

    public static Registration givenRegistrationExistsForUser(final String user) {
        return givenRegistrationExistsForUser(user, identity());
    }

    public static Registration givenRegistrationExistsForUser(final String user,
            final Function<RegistrationBuilder, RegistrationBuilder> transformer) {
        final var registration = Registration.builder()
                .permanentIdentifier(user.getBytes());
        return mongoOperations.save(transformer.apply(registration).build());
    }

    public static Contact givenContactExistForUser(final String user,
            final RobertClock.RobertInstant time,
            final Function<ContactBuilder, ContactBuilder> transformer) {

        var userAndEpoch = (user + ";" + time.asEpochId()).getBytes();
        final var contact = Contact.builder()
                .ebid(userAndEpoch)
                .ecc("fr".getBytes());

        return mongoOperations.save(transformer.apply(contact).build());
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

    @SneakyThrows
    private static void exec(String... args) {
        new ProcessBuilder().command(args)
                .start()
                .waitFor(5, SECONDS);
    }

    public static ListAssert<Registration> assertThatRegistrations() {
        return assertThat(mongoOperations.find(new Query(), Registration.class));
    }

    public static ObjectAssert<Registration> assertThatRegistrationForUser(final String user) {
        return assertThatRegistrations()
                .filteredOn(r -> user.equals(new String(r.getPermanentIdentifier())))
                .hasSize(1)
                .first()
                .as("Registration for user %s", user);
    }

    public static ListAssert<Contact> assertThatContactsToProcess() {
        return assertThat(mongoOperations.find(new Query(), Contact.class))
                .as("Mongodb contact_to_process collection");
    }

    @Value
    @Builder
    public static class HelloMessage {

        String ebid;

        String ecc;

        int time;

        String mac;

        int rssi;

        long receptionTime;
    }
}
