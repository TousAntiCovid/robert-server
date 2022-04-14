package fr.gouv.stopc.robertserver.ws.test;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.test.context.TestExecutionListener;

import java.io.IOException;

public class GrpcMockManager implements TestExecutionListener {

    private static final Server GRPC_MOCK = ServerBuilder.forPort(0)
            .addService(new CryptoGrpcMock())
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

    private static class CryptoGrpcMock extends CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase {

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
            responseObserver.onNext(
                    GetIdFromAuthResponse.newBuilder()
                            .setEpochId(request.getEpochId())
                            .setIdA(request.getEbid())
                            .build()
            );
            responseObserver.onCompleted();
        }

        @Override
        public void getIdFromStatus(GetIdFromStatusRequest request,
                StreamObserver<GetIdFromStatusResponse> responseObserver) {
            responseObserver.onNext(
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
            responseObserver.onCompleted();
        }

        @Override
        public void deleteId(DeleteIdRequest request, StreamObserver<DeleteIdResponse> responseObserver) {
            responseObserver.onNext(
                    DeleteIdResponse.newBuilder()
                            .setIdA(request.getEbid())
                            .build()
            );
            responseObserver.onCompleted();
        }
    }
}
