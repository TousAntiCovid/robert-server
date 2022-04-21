package fr.gouv.stopc.robertserver.ws.test;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GrpcMockManager implements TestExecutionListener {

    private static final Server GRPC_MOCK = ServerBuilder.forPort(0)
            .addService(new CryptoGrpcStub())
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
    public void beforeTestMethod(@NonNull TestContext testContext) throws Exception {
        CryptoGrpcStub.reset();
    }

    public static void givenCryptoServerRaiseError430ForEbid(String ebid) {
        CryptoGrpcStub.EBID_RAISING_430_MISSING_KEY.add(ebid);
    }

    public static void givenCryptoServerRaiseErrorForMacStartingWith(String mac) {
        CryptoGrpcStub.INVALID_MAC.add(mac);
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

        private <T extends GeneratedMessageV3> void handle(ByteString requestEbid, ByteString requestMac,
                T defaultResponse,
                StreamObserver<T> responseObserver) {

            responseObserver.onCompleted();
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
