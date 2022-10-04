package fr.gouv.stopc.robertserver.batch.test;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.database.model.Contact;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ContactFactory {

    public static class ContactBuilder {

        private String idA;

        private CountryCode countryCode = CountryCode.FRANCE;

        private final List<HelloMessageDetail> messageDetails = new ArrayList<>();

        private final MongoOperations mongoOperations;

        private final RobertClock clock;

        public ContactBuilder(MongoOperations mongoOperations, RobertClock robertClock) {
            this.mongoOperations = mongoOperations;
            this.clock = robertClock;
        }

        public ContactBuilder countryCode(final CountryCode countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public ContactBuilder idA(final String idA) {
            this.idA = idA;
            return this;
        }

        public ContactBuilder buildHelloMessage(
                final RobertClock.RobertInstant timeCollectedOnDevice,
                final RobertClock.RobertInstant timeFromHelloMessage,
                final RobertClock.RobertInstant contactEpochId) {
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
            return this;
        }

        public ContactBuilder withValidHelloMessageAt(final RobertClock.RobertInstant instant, final int number) {
            var currentInstant = instant;
            for (int i = 0; i < number; i++) {
                currentInstant = instant.plus(i, ChronoUnit.SECONDS);
                withValidHelloMessageAt(currentInstant);
            }
            return this;
        }

        public ContactBuilder withValidHelloMessageAt(final RobertClock.RobertInstant instant) {
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
                    .map(RobertClock.RobertInstant::asEpochId)
                    .orElse(clock.now().asEpochId());

            final var contact = Contact.builder()
                    .ebid(String.format("%s:%d", this.idA, epochId).getBytes())
                    .ecc(countryCode.asByteArray())
                    .timeInsertion(Instant.now().toEpochMilli())
                    .messageDetails(this.messageDetails)
                    .build();
            return mongoOperations.save(contact);
        }

        private static int randRssi() {
            return ThreadLocalRandom.current().nextInt(-75, -35);
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
}
