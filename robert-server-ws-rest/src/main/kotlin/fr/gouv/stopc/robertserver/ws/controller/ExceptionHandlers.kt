package fr.gouv.stopc.robertserver.ws.controller

import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import fr.gouv.stopc.robertserver.ws.common.base64Encode
import fr.gouv.stopc.robertserver.ws.common.logger
import fr.gouv.stopc.robertserver.ws.service.AuthenticationClockSkewException
import fr.gouv.stopc.robertserver.ws.service.GrpcClientErrorException
import fr.gouv.stopc.robertserver.ws.service.MissingRegistrationException
import fr.gouv.stopc.robertserver.ws.service.RequestRateExceededException
import io.netty.handler.codec.http.HttpResponseStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.validation.MapBindingResult
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletRequest

/**
 * Exception handling requirement from ANSSI is to avoid giving information in error messages.
 *
 * - unexpected errors result in `500 Internal Server Error` HTTP response
 * - out-of-scope use cases result in `400 Bad Request` HTTP response
 * - identity - application registration inconsistency result in `404 Not Found` HTTP response
 * - error due to a missing server key to decrypt EBID+ECC result in a special `430 Missing Server Key` HTTP response
 */

@RestControllerAdvice
class ExceptionHandlers(private val request: HttpServletRequest) : ResponseEntityExceptionHandler() {

    private val log = logger()

    override fun handleExceptionInternal(
        ex: java.lang.Exception,
        body: Any?,
        headers: HttpHeaders,
        status: HttpStatus,
        r: WebRequest
    ): ResponseEntity<Any> {
        if (status.is4xxClientError) {
            log.info("Unacceptable request on ${request.method} ${request.requestURI}: ${ex::class.simpleName} ${ex.message}")
        } else {
            log.error(
                "Unexpected error on ${request.method} ${request.requestURI}: ${ex::class.simpleName} ${ex.message}",
                ex
            )
        }
        return ResponseEntity.status(status).build()
    }

    override fun handleBindException(
        ex: BindException,
        headers: HttpHeaders,
        status: HttpStatus,
        r: WebRequest
    ): ResponseEntity<Any> = handleValidationError(ex)

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatus,
        r: WebRequest
    ): ResponseEntity<Any> = when (val cause = ex.cause) {
        is InvalidFormatException -> {
            val errors = MapBindingResult(mutableMapOf<String, Any>(), "request")
            val field = cause.path.joinToString(".") { it.fieldName }
            errors.rejectValue(field, "InvalidFormatException", cause.originalMessage)
            handleValidationError(errors)
        }
        is MissingKotlinParameterException -> {
            val errors = MapBindingResult(mutableMapOf<String, Any>(), "request")
            errors.rejectValue(cause.parameter.name ?: "", "NotNull", "should not be null")
            handleValidationError(errors)
        }
        is DatabindException -> {
            val errors = MapBindingResult(mutableMapOf<String, Any>(), "request")
            errors.reject(cause.javaClass.simpleName, cause.originalMessage)
            handleValidationError(errors)
        }
        else -> handleExceptionInternal(ex, null, headers, status, r)
    }

    /**
     * Error on unexpected malformed request result in an INFO log message.
     * We use Spring's [BindException] for data validation: [BindingResult] errors are logged.
     * @return an empty HTTP 400 response
     */
    private fun handleValidationError(validation: BindingResult): ResponseEntity<Any> {
        val globalErrors = validation.globalErrors.map { "object '${it.objectName}' ${it.defaultMessage} (${it.code})" }
        val fieldErrors =
            validation.fieldErrors.map { "field '${it.field}' rejected value [${it.rejectedValue?.toPrettyString()}] ${it.defaultMessage} (${it.code})" }
        val errors = (globalErrors + fieldErrors).joinToString(" and ")
        log.info("Request validation failed on ${request.method} ${request.requestURI}: $errors")
        return ResponseEntity.badRequest().build()
    }

    /**
     * Default action on unexpected exception is to log the full stacktrace.
     * @return an empty HTTP 500 response
     */
    @ExceptionHandler(Exception::class)
    fun handleAnyException(ex: Exception, r: WebRequest): ResponseEntity<Any> {
        return handleExceptionInternal(ex, null, HttpHeaders(), INTERNAL_SERVER_ERROR, r)
    }

    /**
     * Communication with GRPC server can result in [GrpcClientErrorException].
     *
     * The GRPC interface contract:
     * - can raise a special status 430 that must be propagated to the client.
     * - raises a 404 error when it can't authenticate the request
     *
     * @return an empty HTTP 430 or 400 response
     */
    @ExceptionHandler(GrpcClientErrorException::class)
    fun handleGrpcClientError(ex: GrpcClientErrorException): ResponseEntity<Void> {
        log.info("Request authentication failed on ${request.method} ${request.requestURI}: ${ex.message}")
        val code = when (ex.code) {
            404 -> 430
            430 -> 430
            else -> HttpResponseStatus.BAD_REQUEST.code()
        }
        return ResponseEntity.status(code).build()
    }

    /**
     * Authentication is rejected because client and server clocks seem to have too much skew.
     * @return an empty HTTP 400 response
     */
    @ExceptionHandler(AuthenticationClockSkewException::class)
    fun handleClockSkew(ex: AuthenticationClockSkewException): ResponseEntity<Void> {
        val diff = ex.serverTime.until(ex.requestTime)
        log.info("Auth denied on ${request.method} ${request.requestURI}: Too much clock skew $diff between client (${ex.requestTime}) and server (${ex.serverTime})")
        return ResponseEntity.badRequest().build()
    }

    /**
     * Authentication succeed but the application registration was missing.
     * @return an empty HTTP 404 response
     */
    @ExceptionHandler(MissingRegistrationException::class)
    fun handleMissingRegistration(ex: MissingRegistrationException): ResponseEntity<Void> {
        log.info("Missing registration on ${request.method} ${request.requestURI}: ${ex.idA}")
        return ResponseEntity.notFound().build()
    }

    /**
     * Request is rejected because throttling is exceeded.
     */
    @ExceptionHandler(RequestRateExceededException::class)
    fun handleRequestThrottleExceeded(ex: RequestRateExceededException): ResponseEntity<Void> {
        log.info("Rejected ${request.method} ${request.requestURI}: ${ex.message}")
        return ResponseEntity.badRequest().build()
    }

    /**
     * Attempt to prettify some values before printing them in logs.
     *
     * - a [ByteArray] is base64 encoded, instead of printing `[99, 111, 110, 116, 101, 110, 116, 32, 116, 101, 115, 116, 32, 49]`, we print `Y29udGVudCB0ZXN0IDE=`
     * - a [List]<[Byte]> is base64 encoded, instead of printing `[99, 111, 110, 116, 101, 110, 116, 32, 116, 101, 115, 116, 32, 49]`, we print `Y29udGVudCB0ZXN0IDE=`
     * - others are [Any.toString]-ified
     */
    private fun Any.toPrettyString() = when (this) {
        is ByteArray -> this.base64Encode()
        is List<*> -> if (this.all { it is Byte }) {
            this.map { it as Byte }
                .toByteArray()
                .base64Encode()
        } else {
            this.toString()
        }

        else -> this.toString()
    }
}
