package fr.gouv.stopc.robertserver.crypto

import com.google.protobuf.ByteString
import fr.gouv.stopc.robertserver.common.ROBERT_EPOCH
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE
import fr.gouv.stopc.robertserver.crypto.test.CountryCode.GERMANY
import fr.gouv.stopc.robertserver.crypto.test.IntegrationTest
import fr.gouv.stopc.robertserver.crypto.test.ValidateContactRequestBuilder.HelloMessageBuilder
import fr.gouv.stopc.robertserver.crypto.test.assertThatWarnLogs
import fr.gouv.stopc.robertserver.crypto.test.clock
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityDoesntExistForIdA
import fr.gouv.stopc.robertserver.crypto.test.givenIdentityExistsForIdA
import fr.gouv.stopc.robertserver.crypto.test.givenValidateContactRequest
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcBinaryField
import fr.gouv.stopc.robertserver.crypto.test.matchers.grpcField
import fr.gouv.stopc.robertserver.crypto.test.matchers.noGrpcError
import fr.gouv.stopc.robertserver.crypto.test.whenRobertCryptoClient
import io.grpc.StatusRuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.HOURS
import java.time.temporal.ChronoUnit.MINUTES
import java.time.temporal.ChronoUnit.SECONDS
import java.util.List

/**
 * Tests server processing that should be done by the crypto server.
 *
 * @see <a href="https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert specification</a> §6.2
 */
@IntegrationTest
class ValidateContactTest {

    private fun givenWellFormedValidateContactRequest(contactInstant: RobertInstant): HelloMessageBuilder {
        return givenValidateContactRequest()
            .idA("BCDEF0A=")
            .countryCode(FRANCE)
            .atEpoch(contactInstant)
            .withHelloMessage()
            .producedAt(contactInstant.plus(1, SECONDS))
            .receivedAfter(Duration.ofMillis(250))
            .andHelloMessage()
            .producedAt(contactInstant.plus(2, SECONDS))
            .receivedAfter(Duration.ofMillis(250))
    }

    @Test
    fun can_validate_contact() {
        val contactInstant = clock.now()
        givenIdentityExistsForIdA("BCDEF0A=")
        val request = givenWellFormedValidateContactRequest(contactInstant)
            .buildRequest()
        val response = whenRobertCryptoClient().validateContact(request)
        assertThat(response)
            .has(noGrpcError())
            .has(grpcBinaryField("idA", "BCDEF0A="))
            .has(grpcField("countryCode", FRANCE.asByteString()))
            .has(grpcField("epochId", contactInstant.asEpochId()))
            .doesNotHave(grpcField("invalidHelloMessageDetails"))
    }

    @Test
    fun should_reject_unexpected_country_codes() {
        givenIdentityExistsForIdA("BCDEF0A=")
        val request = givenWellFormedValidateContactRequest(clock.now())
            .andContact()
            .countryCode(GERMANY)
            .build()
        val response = whenRobertCryptoClient().validateContact(request)
        assertThat(response)
            .has(noGrpcError())
            .doesNotHave(grpcField("idA"))
            .doesNotHave(grpcField("epochId"))
            .has(grpcField("countryCode", GERMANY.asByteString()))
    }

    @Test
    fun cant_validate_contact_with_misencrypted_country_code() {
        givenIdentityExistsForIdA("BCDEF0A=")
        val request = givenWellFormedValidateContactRequest(clock.now())
            .buildRequest()
            .toBuilder()
            .setEcc(ByteString.copyFrom("💥".toByteArray()))
            .build()
        val response = whenRobertCryptoClient().validateContact(request)
        assertThat(response)
            .has(noGrpcError())
            .doesNotHave(grpcField("idA"))
            .doesNotHave(grpcField("epochId"))
            .has(grpcField("countryCode"))
    }

    @Test
    fun should_reject_contact_with_misencrypted_ebid() {
        val contactInstant = clock.now()
        givenIdentityExistsForIdA("BCDEF0A=")
        val request = givenWellFormedValidateContactRequest(contactInstant)
            .buildRequest()
            .toBuilder()
            .setEbid(ByteString.copyFrom("💥".toByteArray()))
            .build()
        assertThatThrownBy {
            whenRobertCryptoClient().validateContact(request)
        }
            .isExactlyInstanceOf(StatusRuntimeException::class.java)
            .hasMessage("UNKNOWN")
        assertThatWarnLogs()
            .contains("Could not decrypt ECC")
    }

    @Test
    fun should_reject_hellomessage_with_epochId_related_to_a_missing_serverkey() {
        val twentyDaysAgo = clock.now().minus(20, DAYS)
        givenIdentityExistsForIdA("BCDEF0A=")
        val request = givenWellFormedValidateContactRequest(twentyDaysAgo)
            .buildRequest()
        val response = whenRobertCryptoClient().validateContact(request)

        // this is not what we really expected, but the EBID is used to encrypt the CC
        // and the CC is the first item verified,
        // then the server behavior is to tell us the contact belongs to another country
        assertThat(response)
            .has(noGrpcError())
            .doesNotHave(grpcField("idA"))
            .doesNotHave(grpcField("epochId"))
            .has(grpcField("countryCode"))
            .doesNotHave(grpcField("countryCode", FRANCE.asByteArray()))
    }

    @Test
    fun cant_validate_contact_when_all_hellomessages_ebid_decryption_fails() {
        val contactInstant = clock.now()
        givenIdentityExistsForIdA("BCDEF0A=")
        val otherContact = givenValidateContactRequest()
            .idA("FFFFFFF=")
            .countryCode(FRANCE)
            .atEpoch(contactInstant)
            .build()

        // Using HelloMessageDetails from a contact with another idA results in wrong
        // MACs values, thus the HelloMessageDetails should all be rejected
        val request = givenValidateContactRequest()
            .idA("BCDEF0A=")
            .countryCode(FRANCE)
            .atEpoch(contactInstant)
            .build()
            .toBuilder()
            .addAllHelloMessageDetails(otherContact.helloMessageDetailsList)
            .build()
        assertThatThrownBy {
            whenRobertCryptoClient().validateContact(request)
        }
            .isExactlyInstanceOf(StatusRuntimeException::class.java)
            .hasMessage("UNKNOWN")
    }

    @Test
    fun cant_validate_contact_when_idA_is_unknown() {
        val contactInstant = clock.now()
        givenIdentityExistsForIdA("BCDEF0A=")
        val request = givenWellFormedValidateContactRequest(contactInstant)
            .buildRequest()
        givenIdentityDoesntExistForIdA("BCDEF0A=")
        assertThatThrownBy {
            whenRobertCryptoClient().validateContact(request)
        }
            .isExactlyInstanceOf(StatusRuntimeException::class.java)
            .hasMessage("UNKNOWN")
        assertThatWarnLogs()
            .contains("Could not find keys for id")
    }

    @Test
    fun should_reject_hellomessage_with_invalid_mac() {
        val contactInstant = clock.now().truncatedTo(ROBERT_EPOCH)
        givenIdentityExistsForIdA("FFFFFFF=")
        givenIdentityExistsForIdA("BCDEF0A=")
        val otherContact = givenValidateContactRequest()
            .idA("FFFFFFF=")
            .countryCode(FRANCE)
            .atEpoch(contactInstant)
            .withHelloMessage()
            .producedAt(contactInstant.plus(1, SECONDS))
            .andHelloMessage()
            .producedAt(contactInstant.plus(2, SECONDS))
            .buildRequest()

        // Using HelloMessageDetails from a contact with another idA results in wrong
        // MACs values, thus the HelloMessageDetails should all be rejected
        val request = givenValidateContactRequest()
            .idA("BCDEF0A=")
            .countryCode(FRANCE)
            .atEpoch(contactInstant)
            .withHelloMessage()
            .producedAt(contactInstant.plus(3, SECONDS))
            .andHelloMessage()
            .producedAt(contactInstant.plus(4, SECONDS))
            .buildRequest()
            .toBuilder()
            .addAllHelloMessageDetails(otherContact.helloMessageDetailsList)
            .build()
        val response = whenRobertCryptoClient().validateContact(request)
        assertThat(response)
            .has(noGrpcError())
            .has(grpcBinaryField("idA", "BCDEF0A="))
            .has(grpcField("countryCode", FRANCE.asByteString()))
            .has(grpcField("epochId", contactInstant.asEpochId()))
            .has(grpcField("invalidHelloMessageDetails", otherContact.helloMessageDetailsList))

        assertThatWarnLogs()
            .contains("MAC is invalid")
    }

    @Nested
    @DisplayName("Should tolerate 1 epoch drift between epoch in the HelloMessage and epoch of the reception time")
    internal inner class StartAndEndOfEpochEdgeCases {
        @ParameterizedTest
        @ValueSource(strings = ["PT-15m", "PT-1s", "PT1s", "PT15m"])
        fun should_accept_hellomessage_received_with_some_advance_or_delay__max_is_1_epoch_drift(
            acceptableReceptionDelay: Duration?
        ) {
            val contactInstant = clock.now()
                .truncatedTo(DAYS)
                .plus(12, HOURS)
                .plus(7, MINUTES)
            givenIdentityExistsForIdA("BCDEF0A=")
            val request = givenValidateContactRequest()
                .idA("BCDEF0A=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)
                .withHelloMessage()
                .producedAt(contactInstant.plus(1, SECONDS))
                .receivedAfter(acceptableReceptionDelay!!)
                .buildRequest()
            val response = whenRobertCryptoClient().validateContact(request)
            assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", "BCDEF0A="))
                .has(grpcField("countryCode", FRANCE.asByteString()))
                .has(grpcField("epochId", contactInstant.asEpochId()))
                .doesNotHave(grpcField("invalidHelloMessageDetails"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["PT-5h", "PT-30m", "PT30m", "PT5h"])
        fun should_reject_hellomessage_received_with_too_much_advance_or_delay(
            unacceptableReceptionDelay: Duration?
        ) {
            val contactInstant = clock.now()
                .truncatedTo(DAYS)
                .plus(12, HOURS)
                .plus(7, MINUTES)
            givenIdentityExistsForIdA("BCDEF0A=")
            val request = givenValidateContactRequest()
                .idA("BCDEF0A=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)
                .withHelloMessage()
                .producedAt(contactInstant.plus(1, SECONDS))
                .andHelloMessage()
                .producedAt(contactInstant)
                .receivedAfter(unacceptableReceptionDelay!!)
                .buildRequest()
            val response = whenRobertCryptoClient().validateContact(request)
            assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", "BCDEF0A="))
                .has(grpcField("countryCode", FRANCE.asByteString()))
                .has(grpcField("epochId", contactInstant.asEpochId()))
                .has(grpcField("invalidHelloMessageDetails", List.of(request.getHelloMessageDetails(1))))
        }
    }

    @Nested
    @DisplayName("Can use previous or next server-key-yyyymmdd when the HelloMessage is received at the early beginning or late end of the day")
    internal inner class StartAndEndOfDayEdgeCases {
        @ParameterizedTest
        @ValueSource(strings = ["PT-3m", "PT-30s", "PT30s", "PT2m59s"])
        fun should_accept_hellomessage_received_less_than_180_seconds_before_or_after_hellomessage_day(
            acceptableReceptionDelay: Duration
        ) {
            val contactInstant = clock.now()
                .truncatedTo(DAYS)
                .plus(
                    if (acceptableReceptionDelay.isNegative) Duration.ZERO else Duration.ofDays(1).minusNanos(1)
                )
            givenIdentityExistsForIdA("BCDEF0A=")
            val request = givenValidateContactRequest()
                .idA("BCDEF0A=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)
                .withHelloMessage()
                .producedAt(contactInstant.plus(1, SECONDS))
                .receivedAfter(acceptableReceptionDelay)
                .buildRequest()
            val response = whenRobertCryptoClient().validateContact(request)
            assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", "BCDEF0A="))
                .has(grpcField("countryCode", FRANCE.asByteString()))
                .has(grpcField("epochId", contactInstant.asEpochId()))
                .doesNotHave(grpcField("invalidHelloMessageDetails"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["PT-1h", "PT-3m-1s", "PT3m1s", "PT1h"])
        fun should_reject_hellomessage_received_more_than_180_seconds_before_or_after_hellomessage_day(
            unacceptableReceptionDelay: Duration
        ) {
            val contactInstant = clock.now()
                .truncatedTo(DAYS)
                .plus(
                    if (unacceptableReceptionDelay.isNegative) Duration.ZERO else Duration.ofDays(1).minusNanos(1)
                )
            givenIdentityExistsForIdA("BCDEF0A=")
            val request = givenValidateContactRequest()
                .idA("BCDEF0A=")
                .countryCode(FRANCE)
                .atEpoch(contactInstant)
                .withHelloMessage()
                .producedAt(contactInstant.plus(1, SECONDS))
                .andHelloMessage()
                .producedAt(contactInstant)
                .receivedAfter(unacceptableReceptionDelay)
                .buildRequest()
            val response = whenRobertCryptoClient().validateContact(request)
            assertThat(response)
                .has(noGrpcError())
                .has(grpcBinaryField("idA", "BCDEF0A="))
                .has(grpcField("countryCode", FRANCE.asByteString()))
                .has(grpcField("epochId", contactInstant.asEpochId()))
                .has(grpcField("invalidHelloMessageDetails", List.of(request.getHelloMessageDetails(1))))
        }
    }
}
