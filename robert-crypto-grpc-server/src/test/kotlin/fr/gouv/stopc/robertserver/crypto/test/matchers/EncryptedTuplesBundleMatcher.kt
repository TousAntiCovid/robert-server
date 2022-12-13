package fr.gouv.stopc.robertserver.crypto.test.matchers

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.server.common.utils.ByteUtils
import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException
import fr.gouv.stopc.robert.server.crypto.structure.impl.CryptoAESGCM
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.common.base64Encode
import fr.gouv.stopc.robertserver.crypto.test.CountryCode
import fr.gouv.stopc.robertserver.crypto.test.cipherForEbidAtEpoch
import fr.gouv.stopc.robertserver.crypto.test.cipherForEcc
import fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcher.EphemeralTuple
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.Condition
import org.assertj.core.api.ObjectArrayAssert
import org.assertj.core.condition.VerboseCondition.verboseCondition
import java.io.IOException
import java.util.Arrays
import java.util.Base64
import kotlin.streams.toList

/**
 * Returns an [EncryptedTuplesBundleMatcher] with an operation to decrypt ephemeral tuples.
 */
fun assertThatTuplesBundle(encryptedTuplesBundle: ByteString): EncryptedTuplesBundleMatcher {
    return EncryptedTuplesBundleMatcher(encryptedTuplesBundle.toByteArray())
}

/**
 * An AssertJ [Condition] to verify the country code of an ephemeral tuple.
 */
fun countryCode(expectedCountryCode: CountryCode): Condition<EphemeralTuple> = verboseCondition(
    { ephemeralTuple -> ephemeralTuple.decryptedCountryCode == expectedCountryCode.numericCode },
    "a tuple with an ECC corresponding to $expectedCountryCode",
    { ephemeralTuple -> " but $ephemeralTuple has an ECC corresponding to ${ephemeralTuple.decryptedCountryCode}" }
)

/**
 * An AssertJ [Condition] to verify the `idA` of an ephemeral tuple.
 */
fun idA(expectedBase64EncodedIdA: String): Condition<EphemeralTuple> = verboseCondition(
    { ephemeralTuple -> expectedBase64EncodedIdA == ephemeralTuple.decryptedBase64IdA },
    "a tuple with an EBID corresponding to idA $expectedBase64EncodedIdA",
    { ephemeralTuple -> " but idA of tuple $ephemeralTuple was ${ephemeralTuple.decryptedBase64IdA}" }
)

/**
 * An AssertJ [Condition] to verify the `idA` of an ephemeral tuple.
 */
fun idA(expectedIdA: ByteArray): Condition<EphemeralTuple> = idA(expectedIdA.base64Encode())

/**
 * An AssertJ [Condition] to verify the ephemeral tuple has the same `epochId` in its cleartext epochId attribute and in the encrypted data of the `ebid`.
 */
fun ebidConstistentWithTupleEpoch(): Condition<EphemeralTuple> = verboseCondition(
    { ephemeralTuple -> ephemeralTuple.epochId == ephemeralTuple.decryptedEbidEpoch },
    "a tuple with an EBID corresponding to the unencrypted epoch",
    { ephemeralTuple -> " but $ephemeralTuple was a tuple with an EBID for epoch ${ephemeralTuple.decryptedEbidEpoch}" }
)

/**
 * An AssertJ [Condition] to verify a list contains `ebid`s for a given continuous period.
 */
fun aBundleWithEpochs(bundleStart: RobertInstant, bundleEnd: RobertInstant): Condition<Array<EphemeralTuple>> {
    val expectedEpochIds = bundleStart.epochsUntil(bundleEnd)
        .map(RobertInstant::asEpochId)
        .toList()
    return verboseCondition(
        { ephemeralTuples ->
            val actualEpochs = ephemeralTuples
                .map { it.epochId }
            actualEpochs == expectedEpochIds
        },
        "a bundle with all epochs from $bundleStart to $bundleEnd",
        { ephemeralTuples -> " but is was a bundle containing epochs:[\n${ephemeralTuples.joinToString(",\n  ")}]" }
    )
}

class EncryptedTuplesBundleMatcher(private val encryptedTuplesBundle: ByteArray) {

    /**
     * Decrypts the encrypted bundle of ephemeral tuples.
     * @return a ListAssert verifiable using conditions [countryCode], [idA], [ebidConstistentWithTupleEpoch], ...
     */
    fun isEncryptedWith(cipherForTuples: CryptoAESGCM): ObjectArrayAssert<EphemeralTuple> {
        try {
            val decryptedTuplesBundle = cipherForTuples.decrypt(encryptedTuplesBundle)
                .decodeToString()
            val listOfEphemeralTuples = Json.decodeFromString<Array<EphemeralTuple>>(decryptedTuplesBundle)
            return assertThat(listOfEphemeralTuples)
                .describedAs("decrypted tuples bundle")
        } catch (e: RobertServerCryptoException) {
            fail<Any>("tuples bundle can't be decrypted", e)
        } catch (e: IOException) {
            fail<Any>("decrypted tuples bundle can't parsed", e)
        }
        throw IllegalStateException()
    }

    @Serializable
    data class EphemeralTuple(
        val epochId: Int,
        val key: TupleKey
    ) {

        @Serializable
        data class TupleKey(
            val ebid: String,
            val ecc: String
        )

        val decryptedEbidEpoch: Int
            get() {
                val ebid = Base64.getDecoder().decode(key.ebid)
                val decryptedEbid = cipherForEbidAtEpoch(epochId).decrypt(ebid)
                val epochId = Arrays.copyOf(decryptedEbid, 3)
                return ByteUtils.convertEpoch24bitsToInt(epochId)
            }

        val decryptedBase64IdA: String
            get() {
                val ebid = Base64.getDecoder().decode(key.ebid)
                val decryptedEbid = cipherForEbidAtEpoch(epochId).decrypt(ebid)
                val idA = Arrays.copyOfRange(decryptedEbid, 3, 8)
                return Base64.getEncoder().encodeToString(idA)
            }

        val decryptedCountryCode: Int
            get() {
                val ebid = Base64.getDecoder().decode(key.ebid)
                val ecc = Base64.getDecoder().decode(key.ecc)

                // Pad to 128-bits
                val payloadToEncrypt = Arrays.copyOf(ebid, 128 / 8)
                // AES Encryption of the payload to encrypt
                val encryptedPayload = cipherForEcc().encrypt(payloadToEncrypt)

                // Truncate to 8 bits
                // Equivalent to MSB in ROBert spec
                val truncatedEncryptedPayload = encryptedPayload[0]
                val truncatedEncryptedEcc = ecc[0]
                return truncatedEncryptedPayload.toInt() xor truncatedEncryptedEcc.toInt()
            }
    }
}
