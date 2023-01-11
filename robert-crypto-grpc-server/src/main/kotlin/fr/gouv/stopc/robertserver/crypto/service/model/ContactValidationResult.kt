package fr.gouv.stopc.robertserver.crypto.service.model

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.HelloMessageDetail

sealed class ContactValidationResult {

    data class UnsupportedCountry(val countryCode: CountryCode) : ContactValidationResult()

    data class ValidContactValidationResult(
        val countryCode: CountryCode,
        val bluetoothIdentifier: BluetoothIdentifier,
        val invalidHelloMessageDetails: List<HelloMessageDetail>
    ) : ContactValidationResult()
}
