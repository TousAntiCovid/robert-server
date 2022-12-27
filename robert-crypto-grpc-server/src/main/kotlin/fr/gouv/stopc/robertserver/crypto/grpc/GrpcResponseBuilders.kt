package fr.gouv.stopc.robertserver.crypto.grpc

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage

object CreateRegistration {

    fun newError(code: Int, description: String) = CreateRegistrationResponse.newBuilder()
        .setError(
            ErrorMessage.newBuilder()
                .setCode(code)
                .setDescription(description)
                .build()
        ).build()
}
