package fr.gouv.stopc.robertserver.crypto.test.matchers;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage;
import org.assertj.core.api.Condition;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.assertj.core.condition.VerboseCondition.verboseCondition;

public class GrpcResponseMatcher {

    public static Condition<GeneratedMessageV3> grpcField(final String name, final Object expectedValue) {
        return verboseCondition(
                response -> expectedValue.equals(extractField(response, name).orElse(null)),
                format("a response having field '%s' = '%s' ", name, expectedValue),
                response -> format(" but it has value '%s'", extractField(response, name).orElse(null))
        );
    }

    public static Condition<GeneratedMessageV3> grpcBinaryField(final String name, final String base64ExpectedValue) {
        return verboseCondition(
                response -> base64ExpectedValue.equals(extractBinaryFieldAsBase64(response, name)),
                format("a response having binary field '%s' = base64Decode(%s) ", name, base64ExpectedValue),
                response -> format(" but it has value base64Decode(%s)", extractBinaryFieldAsBase64(response, name))
        );
    }

    private static String extractBinaryFieldAsBase64(final GeneratedMessageV3 message, final String name) {
        return extractField(message, name)
                .map(v -> (ByteString) v)
                .map(ByteString::toByteArray)
                .map(data -> Base64.getEncoder().encodeToString(data))
                .orElse(null);
    }

    public static Condition<GeneratedMessageV3> noGrpcError() {
        return verboseCondition(
                response -> extractErrorAttribute(response).isEmpty(),
                "a response without error attribute",
                response -> format(" but it has error '%s'", extractErrorAttribute(response).orElse(null))
        );
    }

    public static Condition<GeneratedMessageV3> grpcErrorResponse(final int code, final String descriptionFormat,
            final Object... args) {
        final var description = format(descriptionFormat, args);
        return verboseCondition(
                response -> extractErrorAttribute(response)
                        .map(err -> code == err.getCode() && description.equals(err.getDescription()))
                        .orElse(false),
                format("an error %d '%s'", code, description),
                response -> format(
                        " but it was error %d '%s'",
                        extractErrorAttribute(response).map(ErrorMessage::getCode).orElse(null),
                        extractErrorAttribute(response).map(ErrorMessage::getDescription).orElse(null)
                )
        );
    }

    private static Optional<Object> extractField(final GeneratedMessageV3 message, final String name) {
        return message.getAllFields()
                .entrySet().stream()
                .filter(e -> name.equals(e.getKey().getName()))
                .findFirst()
                .map(Map.Entry::getValue);
    }

    private static Optional<ErrorMessage> extractErrorAttribute(final GeneratedMessageV3 message) {
        return extractField(message, "error")
                .map(v -> (ErrorMessage) v);
    }

}
