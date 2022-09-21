package fr.gouv.stopc.robertserver.batch.test;

import com.mongodb.client.MongoCollection;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.service.RobertClock.RobertInstant;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.Registration.RegistrationBuilder;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Function.identity;
import static lombok.AccessLevel.PRIVATE;
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

    public static Registration givenRegistrationExistsForUser(final String idA) {
        return givenRegistrationExistsForUser(idA, identity());
    }

    public static Registration givenRegistrationExistsForUser(final String idA,
            final Function<RegistrationBuilder, RegistrationBuilder> transformer) {
        final var registration = Registration.builder()
                .permanentIdentifier(idA.getBytes());
        return mongoOperations.save(transformer.apply(registration).build());
    }

    public static Contact givenContactExistForUser(final String user,
            final RobertClock.RobertInstant time,
            final Function<Contact.ContactBuilder, Contact.ContactBuilder> transformer) {

        var userAndEpoch = String.format("%s:%d", user, time.asEpochId()).getBytes();
        final var contact = Contact.builder()
                .ebid(userAndEpoch)
                .ecc(new byte[] { 33 });

        return mongoOperations.save(transformer.apply(contact).build());
    }

    public static void givenPendingContact(final String idA,
            final byte[] ecc,
            Consumer<HelloMessagesBuilder> helloMessagesBuilderConsumer) {
        final var messageDetails = new HelloMessagesBuilder(idA);
        helloMessagesBuilderConsumer.accept(messageDetails);
        final var epochId = messageDetails.messageDetails.stream()
                .findFirst()
                .map(HelloMessageDetail::getMac)
                .map(String::new)
                .map(fakeMac -> fakeMac.replaceFirst("^[^:]*:", ""))
                .map(RobertClock::parse)
                .map(RobertInstant::asEpochId)
                .orElseThrow();
        final var contact = Contact.builder()
                .ebid(String.format("%s:%d", idA, epochId).getBytes())
                .ecc(ecc)
                .timeInsertion(Instant.now().toEpochMilli())
                .messageDetails(messageDetails.build())
                .build();
        mongoOperations.save(contact);
    }

    public static void givenPendingContact(final String idA,
            Consumer<HelloMessagesBuilder> helloMessagesBuilderConsumer) {
        givenPendingContact(idA, new byte[] { 33 }, helloMessagesBuilderConsumer);
    }

    /**
     * Ensure the container is not "paused", "unpausing" it if necessary.
     */
    private static void givenMongodbIsOnline() {
        final var docker = MONGO_DB_CONTAINER.getDockerClient();
        try (final var cmd = docker.inspectContainerCmd(MONGO_DB_CONTAINER.getContainerId())) {
            final var mongodbContainerInspect = cmd.exec();
            if (TRUE.equals(mongodbContainerInspect.getState().getPaused())) {
                docker.unpauseContainerCmd(MONGO_DB_CONTAINER.getContainerId())
                        .exec();
            }
        }
    }

    /**
     * Put the container in "paused" state. Can be used to simulate failure.
     */
    public static void givenMongodbIsOffline() {
        final var docker = MONGO_DB_CONTAINER.getDockerClient();
        try (final var cmd = docker.pauseContainerCmd(MONGO_DB_CONTAINER.getContainerId())) {
            cmd.exec();
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

    public static ObjectAssert<Registration> assertThatRegistrationForUser(final String idA) {
        return assertThatRegistrations()
                .filteredOn(r -> idA.equals(new String(r.getPermanentIdentifier())))
                .hasSize(1)
                .first()
                .as("Registration for idA %s", idA);
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

    @RequiredArgsConstructor(access = PRIVATE)
    public static class HelloMessagesBuilder {

        private final List<HelloMessageDetail> messageDetails = new ArrayList<>();

        private final String idA;

        public HelloMessagesBuilder addAt(final RobertInstant instant) {
            messageDetails.add(
                    HelloMessageDetail.builder()
                            .timeFromHelloMessage((int) instant.asNtpTimestamp())
                            .timeCollectedOnDevice(instant.asNtpTimestamp())
                            .rssiCalibrated(0)
                            .mac(String.format("%s:%s", idA, instant).getBytes())
                            .build()
            );
            return this;
        }

        public HelloMessagesBuilder addAt(final RobertInstant... instants) {
            Arrays.stream(instants).forEach(this::addAt);
            return this;
        }

        private List<HelloMessageDetail> build() {
            return Collections.unmodifiableList(messageDetails);
        }
    }
}
