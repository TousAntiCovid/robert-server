package fr.gouv.stopc.robertserver.crypto.test.matchers

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage
import fr.gouv.stopc.robertserver.common.base64Encode
import org.assertj.core.api.Condition
import org.assertj.core.condition.VerboseCondition.verboseCondition

fun grpcField(name: String): Condition<GeneratedMessageV3> = verboseCondition(
    { response -> extractField(response, name) != null },
    "a response having field '$name'",
    { " but it is missing" }
)

fun grpcField(name: String, expectedValue: Any): Condition<GeneratedMessageV3> = verboseCondition(
    { response -> expectedValue == extractField(response, name) },
    "a response having field '$name' = '$expectedValue'",
    { response -> " but it has value '${extractField(response, name)}'" }
)

fun grpcBinaryField(name: String, base64ExpectedValue: String): Condition<GeneratedMessageV3> = verboseCondition(
    { response -> base64ExpectedValue == extractBinaryFieldAsBase64(response, name) },
    "a response having binary field '$name' = base64Decode($base64ExpectedValue)",
    { response -> " but it has value base64Decode(${extractBinaryFieldAsBase64(response, name)})" }
)

private fun extractBinaryFieldAsBase64(message: GeneratedMessageV3, name: String): String? =
    when (val fieldValue = extractField(message, name)) {
        is ByteString -> fieldValue.toByteArray().base64Encode()
        else -> null
    }

fun noGrpcError(): Condition<GeneratedMessageV3> = verboseCondition(
    { response -> extractErrorAttribute(response) == null },
    "a response without error attribute",
    { response -> " but it has error '${extractErrorAttribute(response)}'" }
)

fun grpcErrorResponse(code: Int, description: String): Condition<GeneratedMessageV3> = verboseCondition(
    { response ->
        val err = extractErrorAttribute(response)
        err != null && err.code == code && err.description == description
    },
    "an error $code '$description'",
    { response ->
        val actualCode = extractErrorAttribute(response)?.code
        val actualDescription = extractErrorAttribute(response)?.description
        " but it was error $actualCode '$actualDescription'"
    }
)

private fun extractField(message: GeneratedMessageV3, name: String): Any? {
    return message.allFields
        .entries
        .find { name == it.key.name }
        ?.value
}

private fun extractErrorAttribute(message: GeneratedMessageV3): ErrorMessage? =
    when (val error = extractField(message, "error")) {
        is ErrorMessage -> error
        else -> null
    }
