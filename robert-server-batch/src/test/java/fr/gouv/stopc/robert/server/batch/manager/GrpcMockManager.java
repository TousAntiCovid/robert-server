package fr.gouv.stopc.robert.server.batch.manager;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.io.IOException;

@Slf4j
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
        log.info("GrpcMock Started");
        System.setProperty("robert.crypto.server.host", "localhost");
        System.setProperty("robert.crypto.server.port", String.valueOf(GRPC_MOCK.getPort()));
    }

    @Override
    @SneakyThrows
    public void beforeTestMethod(@NonNull TestContext testContext) {
        Mockito.reset(CRYPTO_GRPC_STUB);
        CryptoGrpcStub.reset();
    }

    public static void givenCryptoServerCountryCode(ByteString serverCountryCode) {
        CryptoGrpcStub.serverCountryCode = serverCountryCode;
    }

    private static class CryptoGrpcStub extends CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase {

        private static ByteString serverCountryCode = ByteString.EMPTY;

        private static void reset() {
            serverCountryCode = ByteString.EMPTY;
        }

        private ValidateContactResponse handleConfiguredError(ValidateContactRequest request) {
            // Split ebid and epochid
            // TODO : trouver un moyen d'expliciter avant car non trivial.
            var values = request.getEbid().toStringUtf8().split(";");
            var ebid = ByteString.copyFrom(values[0].getBytes());
            var epochId = Integer.valueOf(values[1]);

            if (serverCountryCode != ByteString.EMPTY) {
                return ValidateContactResponse
                        .newBuilder()
                        .setIdA(ebid)
                        .setCountryCode(serverCountryCode)
                        .setEpochId(epochId)
                        .build();
            } else {
                // Return good answer
                return ValidateContactResponse
                        .newBuilder()
                        .setIdA(ebid)
                        .setCountryCode(request.getServerCountryCode())
                        .setEpochId(epochId)
                        .build();
            }
        }

        @Override
        public void validateContact(ValidateContactRequest request,
                StreamObserver<ValidateContactResponse> responseObserver) {
            var response = handleConfiguredError(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
