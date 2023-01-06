package fr.gouv.stopc.robertserver.crypto.service

import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.robertserver.common.model.IdA
import fr.gouv.stopc.robertserver.crypto.repository.KeyRepository
import fr.gouv.stopc.robertserver.crypto.service.model.BluetoothIdentifier
import fr.gouv.stopc.robertserver.crypto.service.model.CountryCode
import fr.gouv.stopc.robertserver.crypto.service.model.EphemeralTuple
import java.security.Key
import java.time.LocalDate
import java.time.ZoneOffset.UTC

class RobertTuplesGenerator(countryCode: Int, private val idA: IdA, private val keyRepository: KeyRepository) {

    companion object {
        private val log = logger()
    }

    private val countryCode = CountryCode(countryCode)

    private val federationKey = keyRepository.getFederationKey()

    fun generate(range: List<RobertInstant>): List<EphemeralTuple> {
        val dailyGenerators = range
            // group epochs by day
            .groupBy { it.asInstant().atZone(UTC).toLocalDate() }
            // fetch server key
            .map {
                DailyTuplesGenerator(
                    date = it.key,
                    serverKey = keyRepository.getServerKey(it.key),
                    epochs = it.value
                )
            }
            .toList()
        val tuples = dailyGenerators
            .filter { it.serverKey != null }
            .flatMap(DailyTuplesGenerator::generate)
        if (range.size != tuples.size) {
            val start = range.first()
            val end = range.last()
            val missingDays = dailyGenerators
                .filter { it.serverKey == null }
                .map(DailyTuplesGenerator::date)
                .joinToString(", ")
            log.warn("Tuples request from $start until $end can't be honored: missing server-keys $missingDays")
        }
        return tuples
    }

    inner class DailyTuplesGenerator(
        val date: LocalDate,
        val serverKey: Key?,
        val epochs: List<RobertInstant>
    ) {

        fun generate() = epochs
            .map(RobertInstant::asEpochId)
            .map { epochId ->
                val ebid = BluetoothIdentifier(epochId, idA).encrypt(serverKey!!)
                val ecc = countryCode.encrypt(federationKey, ebid)
                EphemeralTuple(epochId, ebid, ecc)
            }
    }
}
