package fr.gouv.stopc.robertserver.crypto.test.matchers

import fr.gouv.stopc.robertserver.common.base64Decode
import fr.gouv.stopc.robertserver.common.parseRobertInstant
import fr.gouv.stopc.robertserver.crypto.test.CountryCode.FRANCE
import fr.gouv.stopc.robertserver.crypto.test.CountryCode.GERMANY
import fr.gouv.stopc.robertserver.crypto.test.KEYSTORE
import fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcher.EphemeralTuple
import fr.gouv.stopc.robertserver.crypto.test.matchers.EncryptedTuplesBundleMatcher.EphemeralTuple.TupleKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.crypto.spec.SecretKeySpec

private const val SERVER_KEY_BASE64 = "/T2imPBNO8h7IR3avVJpH6frrqSrarkM"

private const val FEDERATION_KEY_BASE64 = "9ZBbyujJnobnh1raCjXpNiAbQ7XnfMyxn17RbuCft7U="

private val TEST_INSTANT = parseRobertInstant("2022-04-28T09:45:00Z=66855E")

private val VALID_EPHEMERAL_TUPLE = EphemeralTuple(
    66855,
    TupleKey("yqfVsPunrqc=", "wg==")
)

class EncryptedTuplesBundleMatcherTest {

    private val keystoreBackup = ByteArrayOutputStream()

    @BeforeEach
    fun customizeKeystoreKeys() {
        KEYSTORE.store(keystoreBackup, "1234".toCharArray())
        keystoreBackup.close()

        val decodedFederationKey = FEDERATION_KEY_BASE64.base64Decode()
        val federationKey = SecretKeySpec(decodedFederationKey, 0, decodedFederationKey.size, "AES")
        KEYSTORE.setKeyEntry("federation-key", federationKey, "1234".toCharArray(), null)

        val decodedServerKey = SERVER_KEY_BASE64.base64Decode()
        val serverKey = SecretKeySpec(decodedServerKey, 0, decodedServerKey.size, "AES")
        KEYSTORE.setKeyEntry("server-key-20220428", serverKey, "1234".toCharArray(), null)
    }

    @AfterEach
    fun restoreOriginalKeystore() {
        KEYSTORE.load(ByteArrayInputStream(keystoreBackup.toByteArray()), "1234".toCharArray())
    }

    @Test
    fun can_verify_valid_tuple() {
        assertThat(VALID_EPHEMERAL_TUPLE)
            .has(countryCode(FRANCE))
            .has(ebidConstistentWithTupleEpoch())
            .has(idA("30Liauw="))
    }

    @Test
    fun can_detect_inconsistent_epochId() {
        val ebid = "ASdbJwGbxRw="
        val ecc = "wg=="
        assertThatThrownBy {
            assertThat(EphemeralTuple(66855, TupleKey(ebid, ecc)))
                .has(ebidConstistentWithTupleEpoch())
        }
            .hasMessage(
                """

                    Expecting actual:
                      EphemeralTuple(epochId=66855, key=TupleKey(ebid=ASdbJwGbxRw=, ecc=wg==))
                    to have a tuple with an EBID corresponding to the unencrypted epoch but EphemeralTuple(epochId=66855, key=TupleKey(ebid=ASdbJwGbxRw=, ecc=wg==)) was a tuple with an EBID for epoch 3188169
                """.trimIndent()
            )
    }

    @Test
    fun can_detect_invalid_ecc() {
        val ebid = "yqfVsPunrqc="
        val ecc = "Zg=="
        assertThatThrownBy {
            assertThat(EphemeralTuple(66855, TupleKey(ebid, ecc)))
                .has(ebidConstistentWithTupleEpoch())
                .has(countryCode(GERMANY))
        }
            .hasMessage(
                """

                    Expecting actual:
                      EphemeralTuple(epochId=66855, key=TupleKey(ebid=yqfVsPunrqc=, ecc=Zg==))
                    to have a tuple with an ECC corresponding to GERMANY(49) but EphemeralTuple(epochId=66855, key=TupleKey(ebid=yqfVsPunrqc=, ecc=Zg==)) has an ECC corresponding to -123
                """.trimIndent()
            )
    }

    @Test
    fun can_detect_invalid_idA() {
        assertThatThrownBy {
            assertThat<EphemeralTuple>(VALID_EPHEMERAL_TUPLE)
                .has(idA("wrong_idA"))
        }
            .hasMessage(
                """

                    Expecting actual:
                      EphemeralTuple(epochId=66855, key=TupleKey(ebid=yqfVsPunrqc=, ecc=wg==))
                    to have a tuple with an EBID corresponding to idA wrong_idA but idA of tuple EphemeralTuple(epochId=66855, key=TupleKey(ebid=yqfVsPunrqc=, ecc=wg==)) was 30Liauw=
                """.trimIndent()
            )
    }
}
