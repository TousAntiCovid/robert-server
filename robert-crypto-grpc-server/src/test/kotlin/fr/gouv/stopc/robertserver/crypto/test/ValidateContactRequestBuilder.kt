package fr.gouv.stopc.robertserver.crypto.test

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.common.RobertRequestType.HELLO
import fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE
import java.nio.ByteBuffer
import java.time.Duration
import java.util.Arrays
import java.util.Base64
import java.util.stream.Collectors

fun givenValidateContactRequest(): ValidateContactRequestBuilder {
    return ValidateContactRequestBuilder()
}

/**
 * Helper class to build [ValidateContactRequest] grpc requests.
 *
 * Data sent in the request must be encrypted accordingly to the Robert specification.
 * This class aims to provide fluent builders to create requests without considering encryption details.
 *
 * Example: Create a [ValidateContactRequest] grpc request with one HelloMessage produced on 2022-08-14T13:33:42Z and received after 250ms.
 *
 *     val contactInstant = RobertClock("2020-06-01")
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
 *
 * @see <a href="https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert specification 1.1</a>
 */
class ValidateContactRequestBuilder {

    private val helloMessages = mutableListOf<HelloMessageBuilder>()

    private lateinit var countryCode: CountryCode

    private lateinit var base64EncodedIdA: String

    private lateinit var epoch: RobertInstant

    fun countryCode(countryCode: CountryCode): ValidateContactRequestBuilder {
        this.countryCode = countryCode
        return this
    }

    fun idA(base64EncodedIdA: String): ValidateContactRequestBuilder {
        this.base64EncodedIdA = base64EncodedIdA
        return this
    }

    fun atEpoch(epoch: RobertInstant): ValidateContactRequestBuilder {
        this.epoch = epoch
        return this
    }

    fun withHelloMessage(): HelloMessageBuilder {
        return HelloMessageBuilder()
    }

    fun build(): ValidateContactRequest {
        val ebid = encryptedEbid()
        val ecc = encryptedCountryCode()
        return ValidateContactRequest.newBuilder()
            .setServerCountryCode(FRANCE.asByteString())
            .setEcc(ecc)
            .setEbid(ebid)
            .addAllHelloMessageDetails(
                helloMessages.stream()
                    .map { obj: HelloMessageBuilder -> obj.build() }
                    .collect(Collectors.toList())
            )
            .build()
    }

    private fun encryptedEbid(): ByteString {
        val idABytes = Base64.getDecoder().decode(base64EncodedIdA)
        val ebid = ByteBuffer.allocate(8)
            .putInt(epoch.asEpochId())
            .position(1)
            .compact()
            .position(3)
            .put(idABytes)
        val encryptedEbid = cipherForEbidAtEpoch(epoch.asEpochId()).encrypt(ebid.array())
        return ByteString.copyFrom(encryptedEbid)
    }

    private fun encryptedCountryCode(): ByteString {
        val ebid = encryptedEbid().toByteArray()
        // Pad to 128-bits
        val payloadToEncrypt = Arrays.copyOf(ebid, 128 / 8)
        // AES Encryption of the payload to encrypt
        val encryptedPayload = cipherForEcc().encrypt(payloadToEncrypt)
        // Truncate to 8 bits
        // Equivalent to MSB in ROBert spec
        val truncatedEncryptedPayload = encryptedPayload[0]
        val encryptedCountryCode =
            byteArrayOf((truncatedEncryptedPayload.toInt() xor countryCode.asByteArray()[0].toInt()).toByte())
        return ByteString.copyFrom(encryptedCountryCode)
    }

    inner class HelloMessageBuilder {

        private lateinit var productionInstant: RobertInstant
        private var receptionDelay = Duration.ZERO

        fun producedAt(productionInstant: RobertInstant): HelloMessageBuilder {
            this.productionInstant = productionInstant
            return this
        }

        fun receivedAfter(receptionDelay: Duration): HelloMessageBuilder {
            this.receptionDelay = receptionDelay
            return this
        }

        fun build(): HelloMessageDetail? {
            val macInput: ByteArray = ByteBuffer.allocate(12)
                .putInt(8, productionInstant.as16LessSignificantBits())
                .put(HELLO.salt)
                .put(encryptedCountryCode().toByteArray())
                .put(encryptedEbid().toByteArray())
                .array()
            val encryptedMac = getCipherForMac(base64EncodedIdA).encrypt(macInput)
            val mac = encryptedMac.copyOf(5)
            return HelloMessageDetail.newBuilder()
                .setTimeSent(productionInstant.as16LessSignificantBits())
                .setTimeReceived(productionInstant.plus(receptionDelay).asNtpTimestamp())
                .setMac(ByteString.copyFrom(mac))
                .build()
        }

        fun andHelloMessage(): HelloMessageBuilder {
            helloMessages.add(this)
            return HelloMessageBuilder()
        }

        fun andContact(): ValidateContactRequestBuilder {
            return this@ValidateContactRequestBuilder
        }

        fun buildRequest(): ValidateContactRequest {
            helloMessages.add(this)
            return this@ValidateContactRequestBuilder.build()
        }
    }
}
