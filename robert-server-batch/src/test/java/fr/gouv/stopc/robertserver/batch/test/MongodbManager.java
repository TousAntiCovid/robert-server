package fr.gouv.stopc.robertserver.batch.test;

import com.mongodb.client.MongoCollection;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robert.server.common.service.RobertClock.RobertInstant;
import fr.gouv.stopc.robert.server.common.utils.TimeUtils;
import fr.gouv.stopc.robertserver.batch.CountryCode;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.model.Registration.RegistrationBuilder;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.assertj.core.api.*;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static fr.gouv.stopc.robertserver.batch.test.HelloMessageFactory.randRssi;
import static java.lang.Boolean.TRUE;
import static java.util.function.Function.identity;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;

public class MongodbManager implements TestExecutionListener {

    private static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(
            DockerImageName.parse("mongo:4.2.11")
    );

    private static MongoOperations mongoOperations;

    private static RobertClock clock;

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
        clock = testContext.getApplicationContext().getBean(RobertClock.class);
    }

    public static AbstractInstantAssert<?> assertThatLatestRiskEpochForUser(String idA) {
        final var EPOCH_INSTANT = new InstanceOfAssertFactory<>(
                Integer.class, value -> AssertionsForClassTypes.assertThat(clock.atEpoch(value).asInstant())
        );
        return assertThatRegistrationForUser(idA)
                .extracting(Registration::getLatestRiskEpoch, EPOCH_INSTANT)
                .as("Last risk update");
    }

    public static AbstractInstantAssert<?> assertThatLastContactTimestampForUser(String idA) {
        final var NTP_INSTANT = new InstanceOfAssertFactory<>(
                Long.class, value -> AssertionsForClassTypes.assertThat(clock.atNtpTimestamp(value).asInstant())
        );
        return assertThatRegistrationForUser(idA)
                .extracting(Registration::getLastContactTimestamp, NTP_INSTANT)
                .as("Last contact timestamp");
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

    private static ListAssert<Registration> assertThatRegistrations() {
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

    public static GivenPendingContact givenGivenPendingContact() {
        return new GivenPendingContact();
    }

    public static class GivenPendingContact {

        private String idA;

        private CountryCode countryCode = CountryCode.FRANCE;

        private Integer epochId = clock.now().asEpochId();

        private final List<HelloMessageDetail> messageDetails = new ArrayList<>();

        public GivenPendingContact ecc(final CountryCode countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public GivenPendingContact idA(final String idA) {
            this.idA = idA;
            return this;
        }

        private HelloMessageDetail withHelloMessage(final RobertInstant timeCollectedOnDevice,
                final RobertInstant timeFromHelloMessage) {
            return HelloMessageDetail.builder()
                    .rssiCalibrated(randRssi())
                    .timeCollectedOnDevice(
                            timeCollectedOnDevice.asNtpTimestamp()
                    )
                    .timeFromHelloMessage(
                            timeFromHelloMessage.as16LessSignificantBits()
                    )
                    .mac(String.format("%s:%s", idA, clock.now()).getBytes())
                    .build();
        }

        public GivenPendingContact withHelloMessageTimeCollectedIsDivergentFromRegistrationEpochId() {
            final var timeFromHelloMessage = clock.now();
            final var timeCollectedOnDevice = timeFromHelloMessage
                    .plus(TimeUtils.EPOCH_DURATION_SECS * 2L, ChronoUnit.SECONDS);

            this.messageDetails.add(
                    withHelloMessage(timeCollectedOnDevice, timeCollectedOnDevice)
            );
            return this;
        }

        public GivenPendingContact withHelloMessageTimeCollectedExceededMaxTimeStampTolerance() {
            final var HELLO_MESSAGE_TIME_STAMP_TOLERANCE = 180;
            final var timeFromHelloMessage = clock.now();
            final var timeCollectedOnDevice = timeFromHelloMessage
                    .plus(HELLO_MESSAGE_TIME_STAMP_TOLERANCE + 1, ChronoUnit.SECONDS);

            this.messageDetails.add(
                    withHelloMessage(timeCollectedOnDevice, timeFromHelloMessage)
            );
            return this;
        }

        public GivenPendingContact withValidHelloMessages(
                final Consumer<HelloMessagesBuilder> helloMessagesBuilderConsumer) {
            final var messageDetails = new HelloMessagesBuilder(idA);
            helloMessagesBuilderConsumer.accept(messageDetails);

            this.messageDetails.addAll(messageDetails.build());
            return this;
        }

        public Contact build() {
            if (messageDetails.size() > 0) {
                this.epochId = messageDetails.stream()
                        .findFirst()
                        .map(HelloMessageDetail::getMac)
                        .map(String::new)
                        .map(fakeMac -> fakeMac.replaceFirst("^[^:]*:", ""))
                        .map(RobertClock::parse)
                        .map(RobertInstant::asEpochId)
                        .orElseThrow();
            }

            final var contact = Contact.builder()
                    .ebid(String.format("%s:%d", this.idA, this.epochId).getBytes())
                    .ecc(countryCode.asByteArray())
                    .timeInsertion(Instant.now().toEpochMilli())
                    .messageDetails(this.messageDetails)
                    .build();
            return mongoOperations.save(contact);
        }
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
