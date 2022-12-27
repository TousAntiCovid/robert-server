package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robertserver.common.model.IdA
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import javax.crypto.spec.SecretKeySpec

class BluetoothIdentifierTest {

    private val serverKeyYYMMAAAA = "Some example 192 bits daily server-key-DDMMYYYY"
        .toByteArray().take(192 / 8).toByteArray()
        .let { SecretKeySpec(it, "Skinny64") }

    companion object {

        @JvmStatic
        fun ebid_examples() = listOf(
            arguments(1, IdA("AQIDBAU="), "v4lOYvwWzbc="),
            arguments(4532, IdA("BAEFBAU="), "s5MNHJaWmiM="),
            arguments(12345, IdA("+EOzTrc="), "zQH3eRyVxCo="),
            arguments(25843, IdA("9TLsIUk="), "BoxOmm+vcnA="),
            arguments(65432, IdA("ANxLqVE="), "QEp/r7cIHAw="),
        )
    }

    @ParameterizedTest
    @MethodSource("ebid_examples")
    fun can_generate_ebid(epochId: Int, idA: IdA, base64Ebid: String) {
        val ebid = BluetoothIdentifier(epochId, idA)
            .encrypt(serverKeyYYMMAAAA)
        assertThat(ebid.toString())
            .isEqualTo(base64Ebid)
    }
}
