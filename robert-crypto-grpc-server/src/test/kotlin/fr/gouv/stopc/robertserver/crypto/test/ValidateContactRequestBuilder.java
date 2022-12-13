package fr.gouv.stopc.robertserver.crypto.test;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest;
import fr.gouv.stopc.robert.server.common.DigestSaltEnum;
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant;
import lombok.Getter;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE;
import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.cipherForEbidAtEpoch;
import static fr.gouv.stopc.robertserver.crypto.test.KeystoreManager.cipherForEcc;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.getCipherForMac;
import static java.util.stream.Collectors.toList;

/**
 * Helper class to build {@link ValidateContactRequest} grpc requests.
 * <p>
 * Data sent in the request must be encrypted accordingly to the Robert
 * specification. This class aims to provide fluent builders to create requests
 * without considering encryption details.
 * <p>
 * Example: Create a {@link ValidateContactRequest} grpc request with one
 * HelloMessage produced on 2022-08-14T13:33:42Z and received after 250ms.
 * 
 * <pre>
 *     final var contactInstant = new RobertClock("2020-06-01")
 *                 .at(Instant.parse("2022-08-14T13:33:42Z"))
 *     givenValidateContactRequest()
 *                 .idA("BCDEF0A=")
 *                 .countryCode(FRANCE)
 *                 .atEpoch(contactInstant)
 *
 *                 .withHelloMessage()
 *                 .producedAt(contactInstant)
 *                 .receivedAfter(Duration.ofMillis(250))
 *
 *                 .buildContact()
 * </pre>
 *
 * @see <a href=
 *      "https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert
 *      specification 1.1</a>
 */
public class ValidateContactRequestBuilder {

    public static ValidateContactRequestBuilder givenValidateContactRequest() {
        return new ValidateContactRequestBuilder();
    }

    private final List<HelloMessageBuilder> helloMessages = new ArrayList<>();

    private CountryCode countryCode;

    private String base64EncodedIdA;

    @Getter
    private RobertInstant epoch;

    public ValidateContactRequestBuilder countryCode(final CountryCode countryCode) {
        this.countryCode = countryCode;
        return this;
    }

    public ValidateContactRequestBuilder idA(final String base64EncodedIdA) {
        this.base64EncodedIdA = base64EncodedIdA;
        return this;
    }

    public ValidateContactRequestBuilder atEpoch(final RobertInstant epoch) {
        this.epoch = epoch;
        return this;
    }

    public HelloMessageBuilder withHelloMessage() {
        return new HelloMessageBuilder();
    }

    public ValidateContactRequest build() {
        final var ebid = encryptedEbid();
        final var ecc = encryptedCountryCode();
        return ValidateContactRequest.newBuilder()
                .setServerCountryCode(FRANCE.asByteString())
                .setEcc(ecc)
                .setEbid(ebid)
                .addAllHelloMessageDetails(
                        helloMessages.stream()
                                .map(HelloMessageBuilder::build)
                                .collect(toList())
                )
                .build();
    }

    @SneakyThrows
    private ByteString encryptedEbid() {
        final var idABytes = Base64.getDecoder().decode(base64EncodedIdA);
        final var ebid = ByteBuffer.allocate(8)
                .putInt(epoch.asEpochId())
                .position(1)
                .compact()
                .position(3)
                .put(idABytes);
        final var encryptedEbid = cipherForEbidAtEpoch(epoch.asEpochId()).encrypt(ebid.array());
        return ByteString.copyFrom(encryptedEbid);
    }

    @SneakyThrows
    private ByteString encryptedCountryCode() {
        final var ebid = encryptedEbid().toByteArray();
        // Pad to 128-bits
        final var payloadToEncrypt = Arrays.copyOf(ebid, 128 / 8);
        // AES Encryption of the payload to encrypt
        final var encryptedPayload = cipherForEcc().encrypt(payloadToEncrypt);
        // Truncate to 8 bits
        // Equivalent to MSB in ROBert spec
        final var truncatedEncryptedPayload = encryptedPayload[0];

        final var encryptedCountryCode = new byte[] {
                (byte) (truncatedEncryptedPayload ^ countryCode.asByteArray()[0]) };

        return ByteString.copyFrom(encryptedCountryCode);
    }

    public class HelloMessageBuilder {

        private RobertInstant productionInstant;

        private Duration receptionDelay = Duration.ZERO;

        public HelloMessageBuilder producedAt(final RobertInstant productionInstant) {
            this.productionInstant = productionInstant;
            return this;
        }

        public HelloMessageBuilder receivedAfter(final Duration receptionDelay) {
            this.receptionDelay = receptionDelay;
            return this;
        }

        @SneakyThrows
        public HelloMessageDetail build() {
            final var macInput = ByteBuffer.allocate(12)
                    .putInt(8, productionInstant.as16LessSignificantBits())
                    .put(DigestSaltEnum.HELLO.getValue())
                    .put(encryptedCountryCode().toByteArray())
                    .put(encryptedEbid().toByteArray())
                    .array();
            final var encryptedMac = getCipherForMac(base64EncodedIdA).encrypt(macInput);
            final var mac = Arrays.copyOf(encryptedMac, 5);
            return HelloMessageDetail.newBuilder()
                    .setTimeSent(productionInstant.as16LessSignificantBits())
                    .setTimeReceived(productionInstant.plus(receptionDelay).asNtpTimestamp())
                    .setMac(ByteString.copyFrom(mac))
                    .build();
        }

        public HelloMessageBuilder andHelloMessage() {
            helloMessages.add(this);
            return new HelloMessageBuilder();
        }

        public ValidateContactRequestBuilder andContact() {
            return ValidateContactRequestBuilder.this;
        }

        public ValidateContactRequest buildRequest() {
            helloMessages.add(this);
            return ValidateContactRequestBuilder.this.build();
        }
    }
}
