package fr.gouv.stopc.robertserver.ws.test

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusResponse
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import lombok.SneakyThrows
import org.mockito.Mockito
import org.mockito.Mockito.doThrow
import org.mockito.kotlin.anyOrNull
import org.springframework.lang.NonNull
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

class GrpcMockManager : TestExecutionListener {
    @SneakyThrows
    override fun beforeTestMethod(@NonNull testContext: TestContext) {
        Mockito.reset(CRYPTO_GRPC_STUB)
        CryptoGrpcStub.reset()
    }

    companion object {
        private val CRYPTO_GRPC_STUB = Mockito.spy(CryptoGrpcStub())
        private val GRPC_MOCK = ServerBuilder.forPort(0)
            .addService(CRYPTO_GRPC_STUB)
            .build()
            .apply {
                start()
                System.setProperty("robert-ws.crypto-server-uri", "dns:///localhost:$port")
            }

        fun givenCryptoServerIsOffline() {
            val error = RuntimeException("Crypto server is offline")
            doThrow(error).`when`(CRYPTO_GRPC_STUB).createRegistration(anyOrNull(), anyOrNull())
            doThrow(error).`when`(CRYPTO_GRPC_STUB).getIdFromAuth(anyOrNull(), anyOrNull())
            doThrow(error).`when`(CRYPTO_GRPC_STUB).getIdFromStatus(anyOrNull(), anyOrNull())
            doThrow(error).`when`(CRYPTO_GRPC_STUB).deleteId(anyOrNull(), anyOrNull())
        }

        fun givenCryptoServerRaiseMissingIdentityForEbid(ebid: String) {
            CryptoGrpcStub.RESPONSE_STATUS_BY_EBID[ebid] = 404
        }

        fun givenCryptoServerRaiseMissingDailyKeyForEbid(ebid: String) {
            CryptoGrpcStub.RESPONSE_STATUS_BY_EBID[ebid] = 430
        }

        fun givenCryptoServerRaiseError400ForEbid(ebid: String) {
            CryptoGrpcStub.RESPONSE_STATUS_BY_EBID[ebid] = 400
        }

        fun verifyNoInteractionsWithCryptoServer() = Mockito.verifyNoInteractions(CRYPTO_GRPC_STUB)
    }

    private open class CryptoGrpcStub : CryptoGrpcServiceImplImplBase() {

        companion object {
            val RESPONSE_STATUS_BY_EBID: MutableMap<String, Int> = HashMap()
            fun reset() {
                RESPONSE_STATUS_BY_EBID.clear()
            }
        }

        private fun handleConfiguredError(requestEbid: ByteString, requestMac: ByteString): ErrorMessage? {
            return if (RESPONSE_STATUS_BY_EBID.containsKey(requestEbid.toStringUtf8())) {
                ErrorMessage.newBuilder()
                    .setCode(RESPONSE_STATUS_BY_EBID[requestEbid.toStringUtf8()]!!)
                    .setDescription("Some error description")
                    .build()
            } else {
                null
            }
        }

        override fun createRegistration(
            request: CreateRegistrationRequest,
            responseObserver: StreamObserver<CreateRegistrationResponse>
        ) {
            val idA = request.clientPublicKey.toStringUtf8().takeLast(5)
            responseObserver.onNext(
                CreateRegistrationResponse.newBuilder()
                    .setIdA(ByteString.copyFromUtf8(idA))
                    .setTuples(ByteString.copyFromUtf8("fake encrypted tuples for '$idA'"))
                    .build()
            )
            responseObserver.onCompleted()
        }

        override fun getIdFromAuth(
            request: GetIdFromAuthRequest,
            responseObserver: StreamObserver<GetIdFromAuthResponse>
        ) {
            val err = handleConfiguredError(request.ebid, request.mac)
            val response = if (null != err) {
                GetIdFromAuthResponse.newBuilder()
                    .setError(err)
                    .build()
            } else {
                GetIdFromAuthResponse.newBuilder()
                    .setEpochId(request.epochId)
                    .setIdA(request.ebid.take(5))
                    .build()
            }
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }

        override fun getIdFromStatus(
            request: GetIdFromStatusRequest,
            responseObserver: StreamObserver<GetIdFromStatusResponse>
        ) {
            val err = handleConfiguredError(request.ebid, request.mac)
            val response = if (null != err) {
                GetIdFromStatusResponse.newBuilder()
                    .setError(err)
                    .build()
            } else {
                val idA = request.ebid.take(5)
                GetIdFromStatusResponse.newBuilder()
                    .setEpochId(request.epochId)
                    .setIdA(idA)
                    .setTuples(
                        ByteString.copyFromUtf8("fake encrypted tuples for '${idA.toStringUtf8()}'")
                    )
                    .build()
            }
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }

        override fun deleteId(request: DeleteIdRequest, responseObserver: StreamObserver<DeleteIdResponse>) {
            val err = handleConfiguredError(request.ebid, request.mac)
            val response = if (null != err) {
                DeleteIdResponse.newBuilder()
                    .setError(err)
                    .build()
            } else {
                DeleteIdResponse.newBuilder()
                    .setIdA(request.ebid.take(5))
                    .build()
            }
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }

        private fun ByteString.take(n: Int) = ByteString.copyFrom(this.toByteArray().take(n).toByteArray())
    }
}
