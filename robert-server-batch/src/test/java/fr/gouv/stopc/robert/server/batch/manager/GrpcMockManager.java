package fr.gouv.stopc.robert.server.batch.manager;

import com.google.protobuf.ByteString;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest;
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactResponse;
import fr.gouv.stopc.robertserver.database.model.HelloMessageDetail;
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
import java.util.ArrayList;
import java.util.List;

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

    public static void givenCryptoServerRaiseErrorForMacStartingWith(String mac) {
        CryptoGrpcStub.INVALID_MAC.add(mac);
    }

    public static void givenCryptoServerRaiseErrorForThisHelloMessageDetails(HelloMessageDetail helloMessageDetail) {
        CryptoGrpcStub.INVALID_HELLO_MESSAGE_DETAILS.add(helloMessageDetail);
    }

    public static void verifyNoInteractionsWithCryptoServer() {
        Mockito.verifyNoInteractions(CRYPTO_GRPC_STUB);
    }

    public static void givenCryptoServerIdA(ByteString newIdA) {
        CryptoGrpcStub.idA = newIdA;
    }

    public static void givenCryptoServerCountryCode(ByteString serverCountryCode) {
        CryptoGrpcStub.serverCountryCode = serverCountryCode;
    }

    public static void givenCryptoServerEpochId(int epochId) {
        CryptoGrpcStub.epochId = epochId;
    }

    private static class CryptoGrpcStub extends CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase {

        private static ByteString idA = ByteString.EMPTY;

        private static ByteString serverCountryCode = ByteString.EMPTY;

        private static int epochId = 0;

        private static final List<String> ECC = new ArrayList<>();

        private static final List<String> EBID = new ArrayList<>();

        private static final List<String> INVALID_MAC = new ArrayList<>();

        private static final List<HelloMessageDetail> INVALID_HELLO_MESSAGE_DETAILS = new ArrayList<>();

        private static void reset() {
            ECC.clear();
            EBID.clear();
            INVALID_MAC.clear();
            serverCountryCode = ByteString.EMPTY;
        }

        private ValidateContactResponse handleConfiguredError(ValidateContactRequest request) {
            if (!INVALID_HELLO_MESSAGE_DETAILS.isEmpty() &&
                    INVALID_HELLO_MESSAGE_DETAILS.stream().anyMatch(request.getHelloMessageDetailsList()::contains)) {

                var invalidHelloMessageDetails = request.getHelloMessageDetailsList();
                invalidHelloMessageDetails.retainAll(INVALID_HELLO_MESSAGE_DETAILS);

                return ValidateContactResponse
                        .newBuilder()
                        .addAllInvalidHelloMessageDetails(invalidHelloMessageDetails)
                        .setIdA(idA)
                        .setCountryCode(
                                ByteString.copyFromUtf8("fake countryCode")
                        )
                        .setEpochId(666)
                        .build();
            }

            if (serverCountryCode != ByteString.EMPTY) {
                return ValidateContactResponse
                        .newBuilder()
                        .setIdA(idA)
                        .setCountryCode(serverCountryCode)
                        .setEpochId(epochId)
                        .build();
            } else if (EBID.contains(request.getEbid())) {
                return null;
            } else {
                // Return good answer
                return ValidateContactResponse
                        .newBuilder()
                        .setIdA(idA)
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
