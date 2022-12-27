package fr.gouv.stopc.robertserver.crypto.grpc

class RobertCryptoException(val code: Int, val description: String, cause: Throwable? = null) :
    RuntimeException("Status $code: $description", cause)
