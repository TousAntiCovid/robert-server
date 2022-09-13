package fr.gouv.stopc.robertserver.batch.test;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.mockito.Mockito;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

public class GrpcMockManager implements TestExecutionListener {

    private static final CryptoGrpcStub CRYPTO_GRPC_STUB = Mockito.spy(new CryptoGrpcStub());

    private static final Server GRPC_MOCK = ServerBuilder.forPort(0)
            .addService(CRYPTO_GRPC_STUB)
            .build();

    static {
        try {
            GRPC_MOCK.start();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start GRPC mock", e);
        }
        System.setProperty("robert.crypto.server.host", "localhost");
        System.setProperty("robert.crypto.server.port", String.valueOf(GRPC_MOCK.getPort()));
    }

    @Override
    @SneakyThrows
    public void beforeTestMethod(@NonNull TestContext testContext) {
        Mockito.reset(CRYPTO_GRPC_STUB);
        CryptoGrpcStub.reset();
    }

    @SneakyThrows
    public static void givenCryptoServerIsOffline() {
        final var error = new RuntimeException("Crypto server is offline");
        doThrow(error).when(CRYPTO_GRPC_STUB).validateContact(any(), any());
    }

    public static void verifyNoInteractionsWithCryptoServer() {
        Mockito.verifyNoInteractions(CRYPTO_GRPC_STUB);
    }

    public static void givenCryptoServerCountryCode(ByteString serverCountryCode) {
        CryptoGrpcStub.serverCountryCode = serverCountryCode;
    }

    private static class CryptoGrpcStub extends CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase {

        private static ByteString serverCountryCode = ByteString.EMPTY;

        private static void reset() {
            serverCountryCode = ByteString.EMPTY;
        }

        @Override
        public void validateContact(ValidateContactRequest request,
                StreamObserver<ValidateContactResponse> responseObserver) {
            final var ebid = request.getEbid().toStringUtf8();
            final var matcher = Pattern.compile("(?<idA>[^:]+):(?<epochId>\\d+)")
                    .matcher(ebid);
            if (!matcher.find()) {
                responseObserver.onError(new Exception("Mock expects ebid like '{idA}:{epochId}' but is was: " + ebid));
            } else {
                final var idA = matcher.group("idA");
                final var ecc = request.getEcc();
                final var epochId = Integer.parseInt(matcher.group("epochId"));
                var response = ValidateContactResponse.newBuilder()
                        .setIdA(ByteString.copyFromUtf8(idA))
                        .setCountryCode(ecc)
                        .setEpochId(epochId)
                        .addAllInvalidHelloMessageDetails(List.of())
                        .build();
                if (serverCountryCode != ByteString.EMPTY) {
                    response = ValidateContactResponse.newBuilder()
                            .setIdA(ByteString.copyFromUtf8(idA))
                            .setCountryCode(serverCountryCode)
                            .setEpochId(epochId)
                            .addAllInvalidHelloMessageDetails(List.of())
                            .build();
                }
                responseObserver.onNext(response);
            }
            responseObserver.onCompleted();
        }
    }
}
