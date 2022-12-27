package fr.gouv.stopc.robertserver.ws.service

import com.google.protobuf.ByteString
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CreateRegistrationRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.CryptoGrpcServiceImplGrpc.CryptoGrpcServiceImplBlockingStub
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.DeleteIdRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.ErrorMessage
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromAuthRequest
import fr.gouv.stopc.robert.crypto.grpc.server.messaging.GetIdFromStatusRequest
import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.common.RobertClock.RobertInstant
import fr.gouv.stopc.robertserver.common.RobertRequestType
import fr.gouv.stopc.robertserver.ws.RobertWsProperties
import fr.gouv.stopc.robertserver.ws.repository.RegistrationRepository
import fr.gouv.stopc.robertserver.ws.service.model.IdA
import org.springframework.stereotype.Service
import org.springframework.validation.BindException

/**
 * Identifies requests and manage cryptographic identities stored in crypto-server.
 */
@Service
class IdentityService(
    private val clock: RobertClock,
    private val cryptoGrpcService: CryptoGrpcServiceImplBlockingStub,
    private val registrationRepository: RegistrationRepository,
    private val config: RobertWsProperties
) {

    /**
     * Exchange captcha challenge for a new [IdA] _"application identifier"_.
     */
    fun register(publicEcdhKey: ByteArray): EncryptedEphemeralTuplesBundle {
        val createRequest = CreateRegistrationRequest.newBuilder()
            .setServerCountryCode(ByteString.copyFrom(byteArrayOf(config.countryCode.toByte())))
            .setClientPublicKey(ByteString.copyFrom(publicEcdhKey))
            .setFromEpochId(clock.now().asEpochId())
            .setNumberOfDaysForEpochBundles(config.ephemeralTuplesBundleDays)
            .build()
        val response = cryptoGrpcService.createRegistration(createRequest)
        if (response.hasError()) {
            handleGrpcError(response.error)
        }
        return EncryptedEphemeralTuplesBundle(
            IdA(response.idA),
            response.tuples.toList()
        )
    }

    /**
     * Exchange credentials for an [IdA] _"application identifier"_.
     */
    fun authenticate(credentials: RobertCredentials): IdA {
        validateAttributes(credentials)
        val getIdRequest = GetIdFromAuthRequest.newBuilder()
            .setRequestType(credentials.requestType.salt.toInt())
            .setEbid(credentials.ebid.toByteString())
            .setEpochId(credentials.epochId)
            .setTime(clock.atTime32(credentials.time.toByteArray()).asNtpTimestamp())
            .setMac(credentials.mac.toByteString())
            .build()
        val response = cryptoGrpcService.getIdFromAuth(getIdRequest)
        validateTimeSync(credentials, response.idA)
        if (response.hasError()) {
            handleGrpcError(response.error)
        }
        return IdA(response.idA)
    }

    /**
     * Exchange credentials for an [IdA] _"application identifier"_ and a fresh ephemeral tuples bundle.
     */
    fun authenticateAndRenewTuples(credentials: RobertCredentials): EncryptedEphemeralTuplesBundle {
        validateAttributes(credentials)
        val statusRequest = GetIdFromStatusRequest.newBuilder()
            .setServerCountryCode(ByteString.copyFrom(byteArrayOf(config.countryCode.toByte())))
            .setFromEpochId(clock.now().asEpochId())
            .setNumberOfDaysForEpochBundles(config.ephemeralTuplesBundleDays)
            .setEbid(credentials.ebid.toByteString())
            .setEpochId(credentials.epochId)
            .setTime(clock.atTime32(credentials.time.toByteArray()).asNtpTimestamp())
            .setMac(credentials.mac.toByteString())
            .build()
        val response = cryptoGrpcService.getIdFromStatus(statusRequest)
        validateTimeSync(credentials, response.idA)
        if (response.hasError()) {
            handleGrpcError(response.error)
        }
        return EncryptedEphemeralTuplesBundle(
            IdA(response.idA),
            response.tuples.toList()
        )
    }

    /**
     * Exchange credentials for an [IdA] _"application identifier"_ and delete crypto identity.
     */
    fun authenticateAndDeleteIdentity(credentials: RobertCredentials): IdA {
        validateAttributes(credentials)
        val deleteRequest = DeleteIdRequest.newBuilder()
            .setEbid(credentials.ebid.toByteString())
            .setEpochId(credentials.epochId)
            .setTime(clock.atTime32(credentials.time.toByteArray()).asNtpTimestamp())
            .setMac(credentials.mac.toByteString())
            .build()
        val response = cryptoGrpcService.deleteId(deleteRequest)
        validateTimeSync(credentials, response.idA)
        if (response.hasError()) {
            handleGrpcError(response.error)
        }
        return IdA(response.idA)
    }

    /**
     * Surface validation for parameters of an [RobertCredentials].
     * @throws BindException with rejected fields
     */
    private fun validateAttributes(credentials: RobertCredentials) {
        val validation = BindException(credentials, "request")
        if (credentials.ebid.size != 8) {
            validation.rejectValue("ebid", "Size", "must be 8 bytes long")
        }
        if (credentials.time.size != 4) {
            validation.rejectValue("time", "Size", "must be 4 bytes long")
        }
        if (credentials.mac.size != 32) {
            validation.rejectValue("mac", "Size", "must be 32 bytes long")
        }
        if (validation.hasErrors()) {
            throw validation
        }
    }

    /**
     * Client and server time skew must not exceed parameterized threshold.
     */
    private fun validateTimeSync(credentials: RobertCredentials, idA: ByteString) {
        val maxDifference = config.maxAuthRequestClockSkew.abs().seconds
        val serverTime = clock.now()
        val requestTime = clock.atTime32(credentials.time.toByteArray())
        val diff = serverTime.until(requestTime)
        registrationRepository.updateLastTimestampDrift(idA.toByteArray(), diff)
        if (diff.abs().seconds > maxDifference) {
            throw AuthenticationClockSkewException(serverTime, requestTime)
        }
    }

    /**
     * Translate and raise exception for errors returned in Grpc response messages.
     */
    private fun handleGrpcError(error: ErrorMessage) {
        when (error.code) {
            in 400..499 -> throw GrpcClientErrorException(error.code, error.description)
            else -> throw GrpcServerErrorException(error.code, error.description)
        }
    }

    /**
     * Transforms a [List]<[Byte]> to a [ByteString] for GRPC messages.
     */
    private fun List<Byte>.toByteString() = ByteString.copyFrom(this.toByteArray())
}

/**
 * Base class for Grpc errors.
 */
abstract class GrpcClientResponseException(val code: Int, val description: String) :
    RuntimeException("Grpc error status $code: $description")

/**
 * Grpc client error.
 */
class GrpcClientErrorException(code: Int, description: String) : GrpcClientResponseException(code, description)

/**
 * Grpc server error.
 */
class GrpcServerErrorException(code: Int, description: String) : GrpcClientResponseException(code, description)

/**
 * Credentials to be exchanged for an [IdA] _application identifier_.
 *
 * @see <a href="https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert protocol</a> C. Authenticated Requests
 */
data class RobertCredentials(
    val requestType: RobertRequestType,
    val ebid: List<Byte>,
    val epochId: Int,
    val time: List<Byte>,
    val mac: List<Byte>
)

/**
 * The time included in the request drifts too must from the server current time.
 * @see <a href="https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert protocol</a> §6.2. Server Operations
 */
class AuthenticationClockSkewException(
    val serverTime: RobertInstant,
    val requestTime: RobertInstant
) : RuntimeException()

/**
 * Encrypted list of ephemeral tuples for an [IdA].
 * @see <a href="https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert protocol</a> 4. Generation of the Ephemeral Bluetooth Identifiers
 */
data class EncryptedEphemeralTuplesBundle(
    val idA: IdA,
    val encryptedTuples: List<Byte>
)
