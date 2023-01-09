package fr.gouv.stopc.robertserver.crypto.service

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail
import fr.gouv.stopc.robertserver.common.ROBERT_EPOCH
import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.robertserver.crypto.RobertCryptoProperties
import fr.gouv.stopc.robertserver.crypto.grpc.RobertGrpcException
import fr.gouv.stopc.robertserver.crypto.repository.IdentityRepository
import fr.gouv.stopc.robertserver.crypto.repository.KeyRepository
import fr.gouv.stopc.robertserver.crypto.service.model.ContactValidationResult
import fr.gouv.stopc.robertserver.crypto.service.model.ContactValidationResult.UnsupportedCountry
import fr.gouv.stopc.robertserver.crypto.service.model.ContactValidationResult.ValidContactValidationResult
import fr.gouv.stopc.robertserver.crypto.service.model.CountryCode
import fr.gouv.stopc.robertserver.crypto.service.model.Ebid
import fr.gouv.stopc.robertserver.crypto.service.model.Ecc
import fr.gouv.stopc.robertserver.crypto.service.model.HelloMessage
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.MICROS

@Service
class HelloMessageService(
    config: RobertCryptoProperties,
    private val clock: RobertClock,
    private val keyRepository: KeyRepository,
    private val identityRepository: IdentityRepository
) {
    private val log = logger()

    private val maximumReceptionDelay = Duration.of(1, ROBERT_EPOCH)
    private val timestampTolerance = config.helloMessageTimestampTolerance

    fun validate(
        serverCountryCode: CountryCode,
        ebid: Ebid,
        ecc: Ecc,
        helloMessageDetails: List<HelloMessageDetail>
    ): ContactValidationResult {
        val countryCode = ecc.decrypt(keyRepository.getFederationKey(), ebid)
        if (countryCode != serverCountryCode) {
            return UnsupportedCountry(countryCode)
        }

        // decrypt the EBID using first compatible hello details
        val bluetoothIdentifier = helloMessageDetails.firstNotNullOfOrNull { hello ->
            val receptionTime = clock.atNtpTimestamp(hello.timeReceived)
            getPossibleHelloProductionDates(receptionTime)
                .mapNotNull(keyRepository::getServerKey)
                .map(ebid::decrypt)
                .filter { bid -> bid.epochId in getAcceptedEpochs(receptionTime) }
                .firstOrNull()
        } ?: throw RobertGrpcException(400, "Could not decrypt EBID")

        val identity = identityRepository.findByIdA(bluetoothIdentifier.idA.toBase64String())
            ?.decrypt(keyRepository.getKeyEncryptionKey())
            ?: throw RobertGrpcException(400, "Could not find keys for idA")

        val validHelloMessages = helloMessageDetails
            // hellos must be received within an acceptable time span
            .filter { hello -> receivedWithinAcceptedTimeSpan(bluetoothIdentifier.epochId, hello) }
            // hellos must have valid MAC
            .filter { hello ->
                val macIsValid = HelloMessage(ecc, ebid, hello.timeSent)
                    .verifyMac(identity.keyForMac, hello.mac.toByteArray())
                if (!macIsValid) {
                    log.info("MAC is invalid")
                }
                macIsValid
            }

        val invalidHelloMessages = helloMessageDetails - validHelloMessages.toSet()
        return ValidContactValidationResult(countryCode, bluetoothIdentifier, invalidHelloMessages)
    }

    /**
     * Depending on the clock drift between the emitting and the receiving device,
     * we may have to use the previous or the next day server-key-yyyyMMdd to decrypt the EBID.
     */
    private fun getPossibleHelloProductionDates(receptionTime: RobertInstant): Sequence<LocalDate> {
        val receptionDate = receptionTime.toUtcLocalDate()
        val beginOfTheDay = receptionTime.truncatedTo(DAYS)
        val endOfTheDay = beginOfTheDay.plus(1, DAYS)
        return if (Duration.between(beginOfTheDay, receptionTime) <= timestampTolerance) {
            sequenceOf(receptionDate, receptionDate.minusDays(1))
        } else if (Duration.between(receptionTime, endOfTheDay) <= timestampTolerance) {
            sequenceOf(receptionDate, receptionDate.plusDays(1))
        } else {
            sequenceOf(receptionDate)
        }
    }

    /**
     * Get the accepted epochId values for a given Hello Message reception time.
     */
    private fun getAcceptedEpochs(receptionTime: RobertClock.RobertInstant): IntRange {
        val receptionEpochId = receptionTime.asEpochId()
        val minEpochId = receptionEpochId - 1
        val maxEpochId = receptionEpochId + 1
        return minEpochId..maxEpochId
    }

    /**
     * A HelloMessage must be received within the configured timestamp tolerance.
     */
    private fun receivedWithinAcceptedTimeSpan(epochId: Int, helloMessageDetail: HelloMessageDetail): Boolean {
        val productionTime = clock.atEpoch(epochId)
            .withNtp16LeastSignificantBits(helloMessageDetail.timeSent.toUShort())
        val beginOfTheProductionDay = productionTime.truncatedTo(DAYS)
        val endOfTheProductionDay = beginOfTheProductionDay.plus(1, DAYS).minus(1, MICROS)

        val receptionTime = clock.atNtpTimestamp(helloMessageDetail.timeReceived)

        // former implementation use a different tolerance at the beginning and at the end of the days
        return if (receptionTime.isBefore(beginOfTheProductionDay)) {
            Duration.between(receptionTime, beginOfTheProductionDay).abs() <= timestampTolerance
        } else if (receptionTime.isAfter(endOfTheProductionDay)) {
            Duration.between(receptionTime, endOfTheProductionDay).abs() <= timestampTolerance
        } else {
            Duration.between(productionTime, receptionTime).abs() <= maximumReceptionDelay
        }
    }
}
