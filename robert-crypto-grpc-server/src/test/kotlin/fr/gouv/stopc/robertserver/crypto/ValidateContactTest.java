package fr.gouv.stopc.robertserver.crypto;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.ICryptoServerGrpcClient;
import fr.gouv.stopc.robert.crypto.grpc.server.client.service.impl.CryptoServerGrpcClient;
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant;
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest;
import fr.gouv.stopc.robertserver.crypto.test.ValidateContactRequestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;

import static fr.gouv.stopc.robertserver.common.RobertClock.ROBERT_EPOCH;
import static fr.gouv.stopc.robertserver.crypto.test.ClockManager.clock;
import static fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE;
import static fr.gouv.stopc.robertserver.crypto.test.CountryCode.GERMANY;
import static fr.gouv.stopc.robertserver.crypto.test.LogbackManager.assertThatWarnLogs;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.givenIdentityDoesntExistForIdA;
import static fr.gouv.stopc.robertserver.crypto.test.PostgreSqlManager.givenIdentityExistsForIdA;
import static fr.gouv.stopc.robertserver.crypto.test.ValidateContactRequestBuilder.givenValidateContactRequest;
import static fr.gouv.stopc.robertserver.crypto.test.matchers.GrpcResponseMatcher.*;
import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests server processing that should be done by the crypto server.
 *
 * @see <a href=
 * "https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert
 * specification</a> §6.2
 */
@IntegrationTest
class ValidateContactTest {

    private final ICryptoServerGrpcClient robertCryptoClient = new CryptoServerGrpcClient("localhost", 9090);

    private static ValidateContactRequestBuilder.HelloMessageBuilder givenWellFormedValidateContactRequest(
            final RobertInstant contactInstant) {
        return givenValidateContactRequest()
                .idA("BCDEF0A=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)

                .withHelloMessage()
                .producedAt(contactInstant.plus(1, SECONDS))
                .receivedAfter(Duration.ofMillis(250))

                .andHelloMessage()
                .producedAt(contactInstant.plus(2, SECONDS))
                .receivedAfter(Duration.ofMillis(250));
    }

    @Test
    void can_validate_contact() {
        final var contactInstant = clock().now();

        givenIdentityExistsForIdA("BCDEF0A=");

        final var request = givenWellFormedValidateContactRequest(contactInstant)
                .buildRequest();

        final var response = robertCryptoClient.validateContact(request);

        assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", "BCDEF0A="))
                .has(grpcField("countryCode", FRANCE.asByteString()))
                .has(grpcField("epochId", contactInstant.asEpochId()))
                .doesNotHave(grpcField("invalidHelloMessageDetails"));
    }

    @Test
    void should_reject_unexpected_country_codes() {
        givenIdentityExistsForIdA("BCDEF0A=");

        final var request = givenWellFormedValidateContactRequest(clock().now())
                .andContact()
                .countryCode(GERMANY)
                .build();

        final var response = robertCryptoClient.validateContact(request);

        assertThat(response)
                .has(noGrpcError())
                .doesNotHave(grpcField("idA"))
                .doesNotHave(grpcField("epochId"))
                .has(grpcField("countryCode", GERMANY.asByteString()));
    }

    @Test
    void cant_validate_contact_with_misencrypted_country_code() {
        givenIdentityExistsForIdA("BCDEF0A=");

        final var request = givenWellFormedValidateContactRequest(clock().now())
                .buildRequest()
                .toBuilder()
                .setEcc(ByteString.copyFrom("💥".getBytes()))
                .build();

        final var response = robertCryptoClient.validateContact(request);

        assertThat(response)
                .has(noGrpcError())
                .doesNotHave(grpcField("idA"))
                .doesNotHave(grpcField("epochId"))
                .has(grpcField("countryCode"));
    }

    @Test
    void should_reject_contact_with_misencrypted_ebid() {
        final var contactInstant = clock().now();

        givenIdentityExistsForIdA("BCDEF0A=");

        final var request = givenWellFormedValidateContactRequest(contactInstant)
                .buildRequest()
                .toBuilder()
                .setEbid(ByteString.copyFrom("💥".getBytes()))
                .build();

        final var response = robertCryptoClient.validateContact(request);

        assertThat(response).isNull();

        // this is not what we really expected, but the EBID is used to encrypt the CC
        // and the CC is the first item verified,
        // then the logs contains a warning about decrypting the ECC
        assertThatWarnLogs()
                .contains("Could not decrypt ECC");
    }

    @Test
    void should_reject_hellomessage_with_epochId_related_to_a_missing_serverkey() {
        final var twentyDaysAgo = clock().now().minus(20, DAYS);

        givenIdentityExistsForIdA("BCDEF0A=");

        final var request = givenWellFormedValidateContactRequest(twentyDaysAgo)
                .buildRequest();

        final var response = robertCryptoClient.validateContact(request);

        // this is not what we really expected, but the EBID is used to encrypt the CC
        // and the CC is the first item verified,
        // then the server behavior is to tell us the contact belongs to another country
        assertThat(response)
                .has(noGrpcError())
                .doesNotHave(grpcField("idA"))
                .doesNotHave(grpcField("epochId"))
                .has(grpcField("countryCode"))
                .doesNotHave(grpcField("countryCode", FRANCE.asByteArray()));
    }

    @Test
    void cant_validate_contact_when_all_hellomessages_ebid_decryption_fails() {
        final var contactInstant = clock().now();

        givenIdentityExistsForIdA("BCDEF0A=");

        final var otherContact = givenValidateContactRequest()
                .idA("FFFFFFF=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)
                .build();

        // Using HelloMessageDetails from a contact with another idA results in wrong
        // MACs values, thus the HelloMessageDetails should all be rejected
        final var request = givenValidateContactRequest()
                .idA("BCDEF0A=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)
                .build()
                .toBuilder()
                .addAllHelloMessageDetails(otherContact.getHelloMessageDetailsList())
                .build();

        final var response = robertCryptoClient.validateContact(request);

        assertThat(response).isNull();
    }

    @Test
    void cant_validate_contact_when_idA_is_unknown() {
        final var contactInstant = clock().now();

        givenIdentityExistsForIdA("BCDEF0A=");

        final var request = givenWellFormedValidateContactRequest(contactInstant)
                .buildRequest();

        givenIdentityDoesntExistForIdA("BCDEF0A=");

        final var response = robertCryptoClient.validateContact(request);

        assertThat(response).isNull();

        assertThatWarnLogs()
                .containsOnlyOnce("Could not find keys for id");
    }

    @Test
    void should_reject_hellomessage_with_invalid_mac() {
        final var contactInstant = clock().now().truncatedTo(ROBERT_EPOCH);

        givenIdentityExistsForIdA("FFFFFFF=");
        givenIdentityExistsForIdA("BCDEF0A=");

        final var otherContact = givenValidateContactRequest()
                .idA("FFFFFFF=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)

                .withHelloMessage()
                .producedAt(contactInstant.plus(1, SECONDS))

                .andHelloMessage()
                .producedAt(contactInstant.plus(2, SECONDS))

                .buildRequest();

        // Using HelloMessageDetails from a contact with another idA results in wrong
        // MACs values, thus the HelloMessageDetails should all be rejected
        final var request = givenValidateContactRequest()
                .idA("BCDEF0A=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)

                .withHelloMessage()
                .producedAt(contactInstant.plus(3, SECONDS))

                .andHelloMessage()
                .producedAt(contactInstant.plus(4, SECONDS))

                .buildRequest()
                .toBuilder()
                .addAllHelloMessageDetails(otherContact.getHelloMessageDetailsList())
                .build();

        final var response = robertCryptoClient.validateContact(request);

        assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", "BCDEF0A="))
                .has(grpcField("countryCode", FRANCE.asByteString()))
                .has(grpcField("epochId", contactInstant.asEpochId()))
                .has(grpcField("invalidHelloMessageDetails", otherContact.getHelloMessageDetailsList()));
    }

    @Nested
    @IntegrationTest
    @DisplayName("Should tolerate 1 epoch drift between epoch in the HelloMessage and epoch of the reception time")
    class StartAndEndOfEpochEdgeCases {

        @ParameterizedTest
        @ValueSource(strings = { "PT-15m", "PT-1s", "PT1s", "PT15m" })
        void should_accept_hellomessage_received_with_some_advance_or_delay__max_is_1_epoch_drift(
                final Duration acceptableReceptionDelay) {
            final var contactInstant = clock().now()
                    .truncatedTo(DAYS)
                    .plus(12, HOURS)
                    .plus(7, MINUTES);

            givenIdentityExistsForIdA("BCDEF0A=");

            final var request = givenValidateContactRequest()
                    .idA("BCDEF0A=")
                    .countryCode(FRANCE)
                    .atEpoch(contactInstant)

                    .withHelloMessage()
                    .producedAt(contactInstant.plus(1, SECONDS))
                    .receivedAfter(acceptableReceptionDelay)

                    .buildRequest();

            final var response = robertCryptoClient.validateContact(request);

            assertThat(response)
                    .has(noGrpcError())
                    .has(grpcBinaryField("idA", "BCDEF0A="))
                    .has(grpcField("countryCode", FRANCE.asByteString()))
                    .has(grpcField("epochId", contactInstant.asEpochId()))
                    .doesNotHave(grpcField("invalidHelloMessageDetails"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "PT-5h", "PT-30m", "PT30m", "PT5h" })
        void should_reject_hellomessage_received_with_too_much_advance_or_delay(
                final Duration unacceptableReceptionDelay) {
            final var contactInstant = clock().now()
                    .truncatedTo(DAYS)
                    .plus(12, HOURS)
                    .plus(7, MINUTES);

            givenIdentityExistsForIdA("BCDEF0A=");

            final var request = givenValidateContactRequest()
                    .idA("BCDEF0A=")
                    .countryCode(FRANCE)
                    .atEpoch(contactInstant)

                    .withHelloMessage()
                    .producedAt(contactInstant.plus(1, SECONDS))

                    .andHelloMessage()
                    .producedAt(contactInstant)
                    .receivedAfter(unacceptableReceptionDelay)

                    .buildRequest();

            final var response = robertCryptoClient.validateContact(request);

            assertThat(response)
                    .has(noGrpcError())
                    .has(grpcBinaryField("idA", "BCDEF0A="))
                    .has(grpcField("countryCode", FRANCE.asByteString()))
                    .has(grpcField("epochId", contactInstant.asEpochId()))
                    .has(grpcField("invalidHelloMessageDetails", List.of(request.getHelloMessageDetails(1))));
        }
    }

    @Nested
    @IntegrationTest
    @DisplayName("Can use previous or next server-key-yyyymmdd when the HelloMessage is received at the early beginning or late end of the day")
    class StartAndEndOfDayEdgeCases {

        @ParameterizedTest
        @ValueSource(strings = { "PT-3m", "PT-30s", "PT30s", "PT2m59s" })
        void should_accept_hellomessage_received_less_than_180_seconds_before_or_after_hellomessage_day(
                final Duration acceptableReceptionDelay) {
            final var contactInstant = clock().now()
                    .truncatedTo(DAYS)
                    .plus(
                            acceptableReceptionDelay.isNegative()
                                    ? Duration.ZERO
                                    : Duration.ofDays(1).minusNanos(1)
                    );

            givenIdentityExistsForIdA("BCDEF0A=");

            final var request = givenValidateContactRequest()
                    .idA("BCDEF0A=")
                    .countryCode(FRANCE)
                    .atEpoch(contactInstant)

                    .withHelloMessage()
                    .producedAt(contactInstant.plus(1, SECONDS))
                    .receivedAfter(acceptableReceptionDelay)

                    .buildRequest();

            final var response = robertCryptoClient.validateContact(request);

            assertThat(response)
                    .has(noGrpcError())
                    .has(grpcBinaryField("idA", "BCDEF0A="))
                    .has(grpcField("countryCode", FRANCE.asByteString()))
                    .has(grpcField("epochId", contactInstant.asEpochId()))
                    .doesNotHave(grpcField("invalidHelloMessageDetails"));
        }

        @ParameterizedTest
        @ValueSource(strings = { "PT-1h", "PT-3m-1s", "PT3m1s", "PT1h" })
        void should_reject_hellomessage_received_more_than_180_seconds_before_or_after_hellomessage_day(
                final Duration unacceptableReceptionDelay) {

            final var contactInstant = clock().now()
                    .truncatedTo(DAYS)
                    .plus(
                            unacceptableReceptionDelay.isNegative()
                                    ? Duration.ZERO
                                    : Duration.ofDays(1).minusNanos(1)
                    );

            givenIdentityExistsForIdA("BCDEF0A=");

            final var request = givenValidateContactRequest()
                    .idA("BCDEF0A=")
                    .countryCode(FRANCE)
                    .atEpoch(contactInstant)

                    .withHelloMessage()
                    .producedAt(contactInstant.plus(1, SECONDS))

                    .andHelloMessage()
                    .producedAt(contactInstant)
                    .receivedAfter(unacceptableReceptionDelay)

                    .buildRequest();

            final var response = robertCryptoClient.validateContact(request);

            assertThat(response)
                    .has(noGrpcError())
                    .has(grpcBinaryField("idA", "BCDEF0A="))
                    .has(grpcField("countryCode", FRANCE.asByteString()))
                    .has(grpcField("epochId", contactInstant.asEpochId()))
                    .has(grpcField("invalidHelloMessageDetails", List.of(request.getHelloMessageDetails(1))));
        }
    }
}
