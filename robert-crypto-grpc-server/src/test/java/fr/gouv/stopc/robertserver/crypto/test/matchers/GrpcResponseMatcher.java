package fr.gouv.stopc.robertserver.crypto.test.matchers;

import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthResponse;
import org.assertj.core.api.Condition;

import static java.lang.String.format;
import static org.assertj.core.condition.VerboseCondition.verboseCondition;

public class GrpcResponseMatcher {

    public static Condition<CreateRegistrationResponse> noGrpcError() {
        return verboseCondition(
                response -> !response.hasError(),
                "a response without error attribute",
                response -> format(
                        " but it has error %d '%s'", response.getError().getCode(),
                        response.getError().getDescription()
                )
        );
    }

    public static Condition<CreateRegistrationResponse> grpcErrorResponse(int code, String description) {
        return verboseCondition(
                response -> response.hasError()
                        && code == response.getError().getCode()
                        && description.equals(response.getError().getDescription()),
                format("an error %d '%s'", code, description),
                response -> response.hasError() ? format(
                        " but it was error %d '%s'", response.getError().getCode(),
                        response.getError().getDescription()
                )
                        : " but it was not an error"
        );
    }

    public static Condition<GetIdFromAuthResponse> noGetIdFromAuthResponseError() {
        return verboseCondition(
                response -> !response.hasError(),
                "a response without error attribute",
                response -> format(
                        " but it has error %d '%s'", response.getError().getCode(),
                        response.getError().getDescription()
                )
        );
    }

    public static Condition<GetIdFromAuthResponse> getIdFromAuthErrorResponse(int code, String description) {
        return verboseCondition(
                response -> response.hasError()
                        && code == response.getError().getCode()
                        && description.equals(response.getError().getDescription()),
                format("an error %d '%s'", code, description),
                response -> response.hasError() ? format(
                        " but it was error %d '%s'", response.getError().getCode(),
                        response.getError().getDescription()
                )
                        : " but it was not an error"
        );
    }

}
