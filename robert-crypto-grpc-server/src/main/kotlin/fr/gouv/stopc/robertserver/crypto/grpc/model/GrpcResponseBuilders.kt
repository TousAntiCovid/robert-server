package fr.gouv.stopc.robertserver.crypto.grpc.model

import com.google.protobuf.GeneratedMessageV3
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage

interface GrpcErrorMessage<T : GeneratedMessageV3> {
    fun newError(code: Int, description: String): T
}

object CreateRegistration : GrpcErrorMessage<CreateRegistrationResponse> {
    override fun newError(code: Int, description: String) = CreateRegistrationResponse.newBuilder()
        .setError(
            ErrorMessage.newBuilder()
                .setCode(code)
                .setDescription(description)
                .build()
        ).build()
}
