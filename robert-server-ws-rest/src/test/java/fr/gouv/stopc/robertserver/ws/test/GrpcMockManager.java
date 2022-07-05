package fr.gouv.stopc.robertserver.ws.test;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.mockito.Mockito;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        doThrow(error).when(CRYPTO_GRPC_STUB).createRegistration(any(), any());
        doThrow(error).when(CRYPTO_GRPC_STUB).getIdFromAuth(any(), any());
        doThrow(error).when(CRYPTO_GRPC_STUB).getIdFromStatus(any(), any());
        doThrow(error).when(CRYPTO_GRPC_STUB).deleteId(any(), any());
    }

    public static void givenCryptoServerRaiseError430ForEbid(String ebid) {
        CryptoGrpcStub.EBID_RAISING_430_MISSING_KEY.add(ebid);
    }

    public static void givenCryptoServerRaiseErrorForMacStartingWith(String mac) {
        CryptoGrpcStub.INVALID_MAC.add(mac);
    }

    public static void verifyNoInteractionsWithCryptoServer() {
        Mockito.verifyNoInteractions(CRYPTO_GRPC_STUB);
    }

    private static class CryptoGrpcStub extends CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase {

        private static final List<String> EBID_RAISING_430_MISSING_KEY = new ArrayList<>();

        private static final List<String> INVALID_MAC = new ArrayList<>();

        private static void reset() {
            EBID_RAISING_430_MISSING_KEY.clear();
            INVALID_MAC.clear();
        }

        private Optional<ErrorMessage> handleConfiguredError(ByteString requestEbid, ByteString requestMac) {
            if (EBID_RAISING_430_MISSING_KEY.contains(requestEbid.toStringUtf8())) {
                return Optional.of(
                        ErrorMessage.newBuilder()
                                .setCode(430)
                                .setDescription("Some description")
                                .build()
                );
            } else if (INVALID_MAC.stream().anyMatch(invalidMac -> requestMac.toStringUtf8().startsWith(invalidMac))) {
                return Optional.of(
                        ErrorMessage.newBuilder()
                                .setCode(400)
                                .setDescription("Invalid MAC")
                                .build()
                );
            } else {
                return Optional.empty();
            }
        }

        @Override
        public void createRegistration(CreateRegistrationRequest request,
                StreamObserver<CreateRegistrationResponse> responseObserver) {
            final var clientPublicKey = request.getClientPublicKey().toStringUtf8();
            responseObserver.onNext(
                    CreateRegistrationResponse.newBuilder()
                            .setIdA(ByteString.copyFromUtf8("fake idA for " + clientPublicKey))
                            .setTuples(ByteString.copyFromUtf8("fake encrypted tuples for " + clientPublicKey))
                            .build()
            );
            responseObserver.onCompleted();
        }

        @Override
        public void getIdFromAuth(GetIdFromAuthRequest request,
                StreamObserver<GetIdFromAuthResponse> responseObserver) {
            final var response = handleConfiguredError(request.getEbid(), request.getMac())
                    .map(
                            err -> GetIdFromAuthResponse.newBuilder()
                                    .setError(err)
                                    .build()
                    )
                    .orElse(
                            GetIdFromAuthResponse.newBuilder()
                                    .setEpochId(request.getEpochId())
                                    .setIdA(request.getEbid())
                                    .build()
                    );
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void getIdFromStatus(GetIdFromStatusRequest request,
                StreamObserver<GetIdFromStatusResponse> responseObserver) {
            final var response = handleConfiguredError(request.getEbid(), request.getMac())
                    .map(
                            err -> GetIdFromStatusResponse.newBuilder()
                                    .setError(err)
                                    .build()
                    )
                    .orElse(
                            GetIdFromStatusResponse.newBuilder()
                                    .setEpochId(request.getEpochId())
                                    .setIdA(request.getEbid())
                                    .setTuples(
                                            ByteString.copyFromUtf8(
                                                    "fake encrypted tuples for " + request.getEbid().toStringUtf8()
                                            )
                                    )
                                    .build()
                    );
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        @Override
        public void deleteId(DeleteIdRequest request, StreamObserver<DeleteIdResponse> responseObserver) {
            final var response = handleConfiguredError(request.getEbid(), request.getMac())
                    .map(
                            err -> DeleteIdResponse.newBuilder()
                                    .setError(err)
                                    .build()
                    )
                    .orElse(
                            DeleteIdResponse.newBuilder()
                                    .setIdA(request.getEbid())
                                    .build()
                    );
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
