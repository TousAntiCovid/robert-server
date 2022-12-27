package fr.gouv.stopc.robertserver.crypto.grpc

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetInfoFromHelloMessageRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetInfoFromHelloMessageResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.HSMCacheStatusRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.HSMCacheStatusResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ReloadHSMRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ReloadHSMResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ValidateContactResponse
import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.robertserver.crypto.repository.KeyRepository
import fr.gouv.stopc.robertserver.crypto.service.IdentityService
import io.grpc.stub.ServerCalls
import io.grpc.stub.StreamObserver
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service

@Service
class RobertCryptoGrpcService(
    private val clock: RobertClock,
    private val keyRepository: KeyRepository,
    private val identityService: IdentityService
) : CryptoGrpcServiceImplImplBase() {

    private val log = logger()

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "getHSMCacheStatus"])
    override fun getHSMCacheStatus(
        request: HSMCacheStatusRequest,
        responseObserver: StreamObserver<HSMCacheStatusResponse>
    ) = responseObserver.singleItemAnswer {
        HSMCacheStatusResponse.newBuilder()
            .addAllAliases(keyRepository.getCachedKeyNames())
            .build()
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "reloadHSM"])
    override fun reloadHSM(request: ReloadHSMRequest, responseObserver: StreamObserver<ReloadHSMResponse>) =
        responseObserver.singleItemAnswer {
            keyRepository.reloadKeys(request.pin)
            ReloadHSMResponse.newBuilder()
                .setSuccess(true)
                .build()
        }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "createRegistration"])
    override fun createRegistration(
        request: CreateRegistrationRequest,
        responseObserver: StreamObserver<CreateRegistrationResponse>
    ) = responseObserver.singleItemAnswer {
        try {
            val identity = identityService.createIdentity(request.clientPublicKey.toByteArray())

            val tuplesBundle = identityService.generateEncryptedTuplesBundle(
                identity,
                request.serverCountryCode.toByteArray().first().toInt(),
                request.fromEpochId,
                request.numberOfDaysForEpochBundles.toLong()
            )

            CreateRegistrationResponse.newBuilder()
                .setIdA(ByteString.copyFrom(identity.idA.toByteArray()))
                .setTuples(ByteString.copyFrom(tuplesBundle))
                .build()
        } catch (e: RobertCryptoException) {
            if (e.code < 500) {
                log.info(e.message)
            } else {
                log.warn(e.message)
            }
            CreateRegistration.newError(e.code, e.description)
        }
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "getIdFromAuth"])
    override fun getIdFromAuth(
        request: GetIdFromAuthRequest?,
        responseObserver: StreamObserver<GetIdFromAuthResponse>?
    ) {
        super.getIdFromAuth(request, responseObserver)
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "getIdFromStatus"])
    override fun getIdFromStatus(
        request: GetIdFromStatusRequest?,
        responseObserver: StreamObserver<GetIdFromStatusResponse>?
    ) {
        super.getIdFromStatus(request, responseObserver)
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "deleteId"])
    override fun deleteId(request: DeleteIdRequest?, responseObserver: StreamObserver<DeleteIdResponse>?) {
        super.deleteId(request, responseObserver)
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "validateContact"])
    override fun validateContact(
        request: ValidateContactRequest?,
        responseObserver: StreamObserver<ValidateContactResponse>?
    ) {
        super.validateContact(request, responseObserver)
    }

    @Deprecated("to be removed due to performance issues", replaceWith = ReplaceWith("validateContact"))
    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "getInfoFromHelloMessage"])
    override fun getInfoFromHelloMessage(
        request: GetInfoFromHelloMessageRequest?,
        responseObserver: StreamObserver<GetInfoFromHelloMessageResponse>?
    ) = ServerCalls.asyncUnimplementedUnaryCall(
        CryptoGrpcServiceImplGrpc.getGetInfoFromHelloMessageMethod(),
        responseObserver
    )
}
