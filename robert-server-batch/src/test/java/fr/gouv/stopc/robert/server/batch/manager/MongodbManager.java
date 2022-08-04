package fr.gouv.stopc.robert.server.batch.manager;

import com.mongodb.client.MongoCollection;
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

import java.util.List;
import java.util.function.Function;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
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

    public static Contact givenContactExistForUser(final String user) {
        return givenContactExistForUser(user, identity());
    }

    public static Contact givenContactExistForUser(final String user,
            final Function<ContactBuilder, ContactBuilder> transformer) {

        final var contact = Contact.builder()
                .ebid(user.getBytes())
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

    public static List<HelloMessage> helloMessages(final Contact contact) {
        return contact.getMessageDetails()
                .stream()
                .map(
                        hello -> HelloMessage.builder()
                                .ebid(new String(contact.getEbid()))
                                .ecc(new String(contact.getEcc()))
                                .time(hello.getTimeFromHelloMessage())
                                .mac(new String(hello.getMac()))
                                .rssi(hello.getRssiCalibrated())
                                .receptionTime(hello.getTimeCollectedOnDevice())
                                .build()
                )
                .collect(toList());
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
