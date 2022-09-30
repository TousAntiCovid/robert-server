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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static java.util.function.Function.identity;
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
        mongoOperations = testContext.getApplicationContext().getBean(MongoOperations.class);
        mongoOperations.getCollectionNames()
                .stream()
                .map(mongoOperations::getCollection)
                .forEach(MongoCollection::drop);
        clock = testContext.getApplicationContext().getBean(RobertClock.class);
    }

    private static ListAssert<Registration> assertThatRegistrations() {
        return assertThat(mongoOperations.find(new Query(), Registration.class));
    }

    public static ObjectAssert<Registration> assertThatRegistrationForIdA(final String idA) {
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

    public static AbstractListAssert<?, List<?>, Object, ObjectAssert<Object>> assertThatEpochExpositionsForIdA(
            String idA) {
        return assertThatRegistrationForIdA(idA)
                .extracting(Registration::getExposedEpochs)
                .asList();
    }

    public static AbstractInstantAssert<?> assertThatLatestRiskEpochForIdA(String idA) {
        final var EPOCH_INSTANT = new InstanceOfAssertFactory<>(
                Integer.class, value -> AssertionsForClassTypes.assertThat(clock.atEpoch(value).asInstant())
        );
        return assertThatRegistrationForIdA(idA)
                .extracting(Registration::getLatestRiskEpoch, EPOCH_INSTANT)
                .as("Last risk update");
    }

    public static AbstractInstantAssert<?> assertThatLastContactTimestampForIdA(String idA) {
        final var NTP_INSTANT = new InstanceOfAssertFactory<>(
                Long.class, value -> AssertionsForClassTypes.assertThat(clock.atNtpTimestamp(value).asInstant())
        );
        return assertThatRegistrationForIdA(idA)
                .extracting(Registration::getLastContactTimestamp, NTP_INSTANT)
                .as("Last contact timestamp");
    }

    public static Registration givenRegistrationExistsForIdA(final String idA) {
        return givenRegistrationExistsForIdA(idA, identity());
    }

    public static Registration givenRegistrationExistsForIdA(final String idA,
            final Function<RegistrationBuilder, RegistrationBuilder> transformer) {
        final var registration = Registration.builder()
                .permanentIdentifier(idA.getBytes());
        return mongoOperations.save(transformer.apply(registration).build());
    }

    public static ContactBuilder givenGivenPendingContact() {
        return new ContactBuilder();
    }

    public static class ContactBuilder {

        private String idA;

        private CountryCode countryCode = CountryCode.FRANCE;

        private final List<HelloMessageDetail> messageDetails = new ArrayList<>();

        public ContactBuilder countryCode(final CountryCode countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public ContactBuilder idA(final String idA) {
            this.idA = idA;
            return this;
        }

        private void buildHelloMessage(final RobertInstant timeCollectedOnDevice,
                final RobertInstant timeFromHelloMessage,
                final RobertInstant contactEpochId) {
            this.messageDetails.add(
                    HelloMessageDetail.builder()
                            .rssiCalibrated(randRssi())
                            .timeCollectedOnDevice(
                                    timeCollectedOnDevice.asNtpTimestamp()
                            )
                            .timeFromHelloMessage(
                                    timeFromHelloMessage.as16LessSignificantBits()
                            )
                            .mac(String.format("%s:%s", idA, contactEpochId).getBytes())
                            .build()
            );
        }

        /**
         * This helloMessage has a timeCollectedOnDevice which is diverging from
         * Registration EpochId (1800s)
         */
        public ContactBuilder withHelloMessageWithDivergentTimeCollected(RobertInstant instant) {
            final var timeCollectedOnDevice = instant
                    .plus(TimeUtils.EPOCH_DURATION_SECS * 2L, ChronoUnit.SECONDS);
            final var timeFromHelloMessage = timeCollectedOnDevice;
            final var contactEpochId = instant;

            buildHelloMessage(timeCollectedOnDevice, timeFromHelloMessage, contactEpochId);
            return this;
        }

        /**
         * This helloMessage has a TimeCollectedOnDevice higher than the max timestamp
         * tolerance (181s)
         */
        public ContactBuilder withHelloMessageWithBadTimeCollectedOnDevice(RobertInstant instant) {
            final var HELLO_MESSAGE_TIME_STAMP_TOLERANCE = 180;
            final var timeFromHelloMessage = instant;
            final var timeCollectedOnDevice = timeFromHelloMessage
                    .plus(HELLO_MESSAGE_TIME_STAMP_TOLERANCE + 1, ChronoUnit.SECONDS);
            final var contactEpochId = instant;

            buildHelloMessage(timeCollectedOnDevice, timeFromHelloMessage, contactEpochId);
            return this;
        }

        public ContactBuilder withValidHelloMessageAt(final RobertInstant instant, final int number) {
            var currentInstant = instant;
            for (int i = 0; i < number; i++) {
                currentInstant = instant.plus(i, ChronoUnit.SECONDS);
                withValidHelloMessageAt(currentInstant);
            }
            return this;
        }

        public ContactBuilder withValidHelloMessageAt(final RobertInstant instant) {
            messageDetails.add(
                    HelloMessageDetail.builder()
                            .timeFromHelloMessage((int) instant.asNtpTimestamp())
                            .timeCollectedOnDevice(instant.plus(60, ChronoUnit.SECONDS).asNtpTimestamp())
                            .rssiCalibrated(randRssi())
                            .mac(String.format("%s:%s", idA, instant).getBytes())
                            .build()
            );
            return this;
        }

        public Contact build() {
            var epochId = messageDetails.stream()
                    .findFirst()
                    .map(HelloMessageDetail::getMac)
                    .map(String::new)
                    .map(fakeMac -> fakeMac.replaceFirst("^[^:]*:", ""))
                    .map(RobertClock::parse)
                    .map(RobertInstant::asEpochId)
                    .orElse(clock.now().asEpochId());

            final var contact = Contact.builder()
                    .ebid(String.format("%s:%d", this.idA, epochId).getBytes())
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

    private static int randRssi() {
        return ThreadLocalRandom.current().nextInt(-75, -35);
    }
}
