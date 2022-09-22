package fr.gouv.stopc.robertserver.crypto.test.matchers;

import com.google.protobuf.GeneratedMessageV3;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage;
import org.assertj.core.api.Condition;

import java.util.Optional;

import static java.lang.String.format;
import static org.assertj.core.condition.VerboseCondition.verboseCondition;

public class GrpcResponseMatcher {

    public static <T extends GeneratedMessageV3> Condition<T> noGrpcError() {
        return verboseCondition(
                response -> extractErrorAttribute(response).isEmpty(),
                "a response without error attribute",
                response -> format(" but it has error '%s'", extractErrorAttribute(response))
        );
    }

    public static <T extends GeneratedMessageV3> Condition<T> grpcErrorResponse(int code, String descriptionFormat,
            Object... args) {
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

    private static Optional<ErrorMessage> extractErrorAttribute(GeneratedMessageV3 message) {
        return message.getAllFields()
                .entrySet().stream()
                .filter(e -> "error".equals(e.getKey().getName()))
                .findFirst()
                .map(e -> (ErrorMessage) e.getValue());
    }

}
