package fr.gouv.stopc.robertserver.crypto.service

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.model.IdA
import fr.gouv.stopc.robertserver.crypto.repository.KeyRepository
import fr.gouv.stopc.robertserver.crypto.service.model.Ebid
import fr.gouv.stopc.robertserver.crypto.service.model.Ecc
import fr.gouv.stopc.robertserver.crypto.service.model.EphemeralTuple
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.time.LocalDate
import javax.crypto.spec.SecretKeySpec

@ExtendWith(MockitoExtension::class)
class RobertTuplesGeneratorTest(
    @Mock private val keyRepository: KeyRepository
) {

    private val federationKey = SecretKeySpec("Federation key for java unit tests".take(32).toByteArray(), "AES")
    private val serverKey = SecretKeySpec("Secret key for java unit tests".take(24).toByteArray(), "Skinny64")
    private val clock = RobertClock("2022-08-01")

    private lateinit var generator: RobertTuplesGenerator

    @BeforeEach
    fun setup() {
        Mockito.`when`(keyRepository.getFederationKey())
            .thenReturn(federationKey)
        Mockito.`when`(keyRepository.getServerKey(any()))
            .thenReturn(serverKey)
        generator = RobertTuplesGenerator(33, IdA("8nPoETw="), keyRepository)
    }

    @Test
    fun can_generate_tuples_across_multiple_days() {
        val begin = clock.atEpoch(100)
        val end = clock.atEpoch(200)

        val tuples = generator.generate(begin.epochsUntil(end).toList())

        assertThat(tuples)
            .hasSize(100)
    }

    @Test
    fun can_generate_expected_tuple() {
        val begin = clock.atEpoch(100)
        val end = clock.atEpoch(103)

        val tuples = generator.generate(begin.epochsUntil(end).toList())

        assertThat(tuples)
            .containsExactly(
                EphemeralTuple(100, Ebid("TnO2cc+3OkY="), Ecc("lw==")),
                EphemeralTuple(101, Ebid("CRnzbpXDc3s="), Ecc("lw==")),
                EphemeralTuple(102, Ebid("yMpqQuVTDxk="), Ecc("yQ=="))
            )
    }

    @Test
    fun can_skip_tuples_for_a_day_missing_server_key() {
        Mockito.`when`(keyRepository.getServerKey(eq(LocalDate.parse("2022-08-01"))))
            .thenReturn(null)

        val begin = clock.atEpoch(0)
        val end = clock.atEpoch(200)

        val tuples = generator.generate(begin.epochsUntil(end).toList())

        // day 1 | day 2 | day 3
        // ___96 | ___96 | ____8 tuples
        // ____❌ | ____✔ | ____✔
        // ____0 | ___96 | ____8 tuples
        // ➡ 104
        assertThat(tuples)
            .hasSize(104)
    }

    @Test
    fun can_skip_tuples_for_a_day_missing_server_key_in_the_middle_of_the_bundle() {
        Mockito.`when`(keyRepository.getServerKey(eq(LocalDate.parse("2022-08-02"))))
            .thenReturn(null)

        val begin = clock.atEpoch(95)
        val end = clock.atEpoch(193)

        val tuples = generator.generate(begin.epochsUntil(end).toList())

        // day 1 | day 2 | day 3
        // ____1 | ___96 | ____1 tuples
        // ____✔ | ____❌ | ____✔
        // ____1 | ____0 | ____1 tuples
        // ➡ 2
        assertThat(tuples)
            .hasSize(2)
    }
}
