package fr.gouv.stopc.robertserver.crypto.service.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import javax.crypto.spec.SecretKeySpec

class CountryCodeTest {

    private val federationKey = "Some example 256 bits federation key"
        .toByteArray().take(192 / 8).toByteArray()
        .let { SecretKeySpec(it, "AES") }

    companion object {

        @JvmStatic
        fun country_codes_examples() = listOf(
            Arguments.arguments(33, Ebid("v4lOYvwWzbc="), "uQ=="),
            Arguments.arguments(33, Ebid("s5MNHJaWmiM="), "hg=="),
            Arguments.arguments(49, Ebid("v4lOYvwWzbc="), "qQ=="),
            Arguments.arguments(49, Ebid("s5MNHJaWmiM="), "lg==")
        )
    }

    @ParameterizedTest
    @MethodSource("country_codes_examples")
    fun can_encrypt_country_code(countryCode: Int, ebid: Ebid, base64Ecc: String) {
        val ecc = CountryCode(countryCode).encrypt(federationKey, ebid)
        assertThat(ecc.toString())
            .isEqualTo(base64Ecc)
    }
}
