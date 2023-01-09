package fr.gouv.stopc.robertserver.crypto.grpc

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplImplBase
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdResponse
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage
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
import fr.gouv.stopc.robertserver.common.RobertRequestType
import fr.gouv.stopc.robertserver.common.RobertRequestType.STATUS
import fr.gouv.stopc.robertserver.common.RobertRequestType.UNREGISTER
import fr.gouv.stopc.robertserver.common.logger
import fr.gouv.stopc.robertserver.common.model.IdA
import fr.gouv.stopc.robertserver.crypto.repository.KeyRepository
import fr.gouv.stopc.robertserver.crypto.service.HelloMessageService
import fr.gouv.stopc.robertserver.crypto.service.IdentityService
import fr.gouv.stopc.robertserver.crypto.service.model.AuthMac
import fr.gouv.stopc.robertserver.crypto.service.model.ContactValidationResult.UnsupportedCountry
import fr.gouv.stopc.robertserver.crypto.service.model.ContactValidationResult.ValidContactValidationResult
import fr.gouv.stopc.robertserver.crypto.service.model.CountryCode
import fr.gouv.stopc.robertserver.crypto.service.model.Credentials
import fr.gouv.stopc.robertserver.crypto.service.model.Ebid
import fr.gouv.stopc.robertserver.crypto.service.model.Ecc
import io.grpc.stub.ServerCalls
import io.grpc.stub.StreamObserver
import io.micrometer.core.annotation.Timed
import org.springframework.stereotype.Service

@Service
class RobertCryptoGrpcService(
    private val clock: RobertClock,
    private val keyRepository: KeyRepository,
    private val identityService: IdentityService,
    private val helloMessageService: HelloMessageService
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
        val identity = identityService.createIdentity(request.clientPublicKey.toByteArray())

        val tuplesBundle = identityService.generateEncryptedTuplesBundle(
            identity,
            request.serverCountryCode.toByteArray().first().toInt(),
            request.fromEpochId,
            request.numberOfDaysForEpochBundles.toLong()
        )

        CreateRegistrationResponse.newBuilder()
            .setIdA(identity.idA.toByteString())
            .setTuples(tuplesBundle.toByteString())
            .build()
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "getIdFromAuth"])
    override fun getIdFromAuth(
        request: GetIdFromAuthRequest,
        responseObserver: StreamObserver<GetIdFromAuthResponse>
    ) = responseObserver.singleItemAnswer {
        val credentials =
            validateCredentials(request.requestType, request.epochId, request.time, request.ebid, request.mac)
        val identity = identityService.authenticate(credentials)
        GetIdFromAuthResponse.newBuilder()
            .setIdA(identity.idA.toByteString())
            .setEpochId(request.epochId)
            .build()
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "getIdFromStatus"])
    override fun getIdFromStatus(
        request: GetIdFromStatusRequest,
        responseObserver: StreamObserver<GetIdFromStatusResponse>
    ) = responseObserver.singleItemAnswer {
        val credentials = validateCredentials(STATUS, request.epochId, request.time, request.ebid, request.mac)
        val identity = identityService.authenticate(credentials)
        val tuplesBundle = identityService.generateEncryptedTuplesBundle(
            identity,
            request.serverCountryCode.toByteArray().first().toInt(),
            request.fromEpochId,
            request.numberOfDaysForEpochBundles.toLong()
        )
        GetIdFromStatusResponse.newBuilder()
            .setIdA(identity.idA.toByteString())
            .setEpochId(request.epochId)
            .setTuples(tuplesBundle.toByteString())
            .build()
    }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "deleteId"])
    override fun deleteId(request: DeleteIdRequest, responseObserver: StreamObserver<DeleteIdResponse>) =
        responseObserver.singleItemAnswer {
            val credentials = validateCredentials(UNREGISTER, request.epochId, request.time, request.ebid, request.mac)
            val identity = identityService.authenticate(credentials)
            identityService.delete(identity.idA)

            DeleteIdResponse.newBuilder()
                .setIdA(identity.idA.toByteString())
                .build()
        }

    @Timed(value = "robert.crypto.rpc", extraTags = ["operation", "validateContact"])
    override fun validateContact(
        request: ValidateContactRequest,
        responseObserver: StreamObserver<ValidateContactResponse>
    ) = responseObserver.singleItemAnswer {
        val serverCountryCode = CountryCode(request.serverCountryCode.first().toInt())
        val ebid = try {
            Ebid(request.ebid.toByteArray().toList())
        } catch (e: IllegalArgumentException) {
            throw RobertGrpcException(400, "Invalid EBID", "${e.message}")
        }
        val ecc = try {
            Ecc(request.ecc.toByteArray())
        } catch (e: IllegalArgumentException) {
            throw RobertGrpcException(400, "Invalid ECC", "${e.message}")
        }

        val validatedContact =
            helloMessageService.validate(serverCountryCode, ebid, ecc, request.helloMessageDetailsList)

        when (validatedContact) {
            is UnsupportedCountry -> ValidateContactResponse.newBuilder()
                .setCountryCode(validatedContact.countryCode.toByteString())
                .build()

            is ValidContactValidationResult -> ValidateContactResponse.newBuilder()
                .setCountryCode(validatedContact.countryCode.toByteString())
                .setIdA(validatedContact.bluetoothIdentifier.idA.toByteString())
                .setEpochId(validatedContact.bluetoothIdentifier.epochId)
                .addAllInvalidHelloMessageDetails(validatedContact.invalidHelloMessageDetails)
                .build()
        }
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

    /**
     * Surface validation for credentials.
     */
    private fun validateCredentials(
        requestType: Int,
        epochId: Int,
        ntpTimestampSeconds: Long,
        ebid: ByteString,
        mac: ByteString
    ): Credentials {
        val type = RobertRequestType.fromValue(requestType)
            ?: throw RobertGrpcException(400, "Unknown request type: $requestType")
        return validateCredentials(type, epochId, ntpTimestampSeconds, ebid, mac)
    }

    /**
     * Surface validation for credentials.
     */
    private fun validateCredentials(
        type: RobertRequestType,
        epochId: Int,
        ntpTimestampSeconds: Long,
        rawEbid: ByteString,
        rawMac: ByteString
    ): Credentials {
        if (epochId < 0) {
            throw RobertGrpcException(400, "Negative epoch id: $epochId")
        }
        val instant = clock.atNtpTimestamp(ntpTimestampSeconds)
        val ebid = try {
            Ebid(rawEbid.toByteArray().toList())
        } catch (e: IllegalArgumentException) {
            throw RobertGrpcException(400, "Invalid EBID", "${e.message}")
        }
        val mac = try {
            AuthMac(rawMac.toByteArray().toList())
        } catch (e: IllegalArgumentException) {
            throw RobertGrpcException(400, "Invalid MAC", "${e.message}")
        }
        return Credentials(type, epochId, instant, ebid, mac)
    }

    /**
     * Returns this [String] as a GRPC [ByteString].
     */
    private fun String.toByteString() = ByteString.copyFromUtf8(this)

    /**
     * Returns this [ByteArray] as a GRPC [ByteString].
     */
    private fun ByteArray.toByteString() = ByteString.copyFrom(this)

    /**
     * Returns this [CountryCode] as a GRPC [ByteString].
     */
    private fun CountryCode.toByteString() = byteArrayOf(this.numericValue.toByte()).toByteString()

    /**
     * Returns this [IdA] as a GRPC [ByteString].
     */
    private fun IdA.toByteString() = this.toByteArray().toByteString()

    /**
     * Boilerplate to respond a single item to a GRPC request and handle [RobertGrpcException].
     */
    private inline fun <reified V : GeneratedMessageV3> StreamObserver<V>.singleItemAnswer(handler: () -> V) =
        try {
            onNext(handler())
            onCompleted()
        } catch (e: RobertGrpcException) {
            if (e.code < 500) {
                log.info(e.message + if (e.details != "") e.details.prependIndent(": ") else "")
            } else {
                log.error(e.message + if (e.details != "") e.details.prependIndent(": ") else "")
            }
            val err = ErrorMessage.newBuilder()
                .setCode(e.code)
                .setDescription(e.description)
                .build()
            val errorResponse = when (V::class) {
                CreateRegistrationResponse::class -> CreateRegistrationResponse.newBuilder().setError(err).build()
                GetIdFromAuthResponse::class -> GetIdFromAuthResponse.newBuilder().setError(err).build()
                GetIdFromStatusResponse::class -> GetIdFromStatusResponse.newBuilder().setError(err).build()
                DeleteIdResponse::class -> DeleteIdResponse.newBuilder().setError(err).build()
                else -> null
            }
            if (errorResponse != null) {
                onNext(errorResponse as V)
                onCompleted()
            } else {
                onError(e)
            }
        } catch (e: Exception) {
            log.error("An exception occurred while handling response", e)
            onError(e)
        }
}
