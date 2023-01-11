package fr.gouv.stopc.robertserver.crypto.grpc

/**
 * Generic exception to reject/stop request processing.
 */
class RobertGrpcException(
    /**
     * An error code like HTTP status codes.
     */
    val code: Int,

    /**
     * A brief description that may be sent back to the client.
     */
    val description: String,

    /**
     * Some details that mey be logged for debugging purposes.
     */
    val details: String = "",
    cause: Throwable? = null
) : RuntimeException("Status $code: $description", cause)
