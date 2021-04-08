package fr.gouv.clea.ws.exception;

import fr.gouv.clea.ws.dto.ApiError;
import fr.gouv.clea.ws.dto.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ControllerAdvice
@Slf4j
public class CleaRestExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String ERROR_MESSAGE_TEMPLATE = "%s, requested uri: %s";

    @ExceptionHandler(CleaBadRequestException.class)
    public ResponseEntity<ApiError> handleCleaBadRequestException(CleaBadRequestException ex, WebRequest webRequest) {
        final HttpStatus status = getHttpStatus(ex);
        log.error(String.format(ERROR_MESSAGE_TEMPLATE, ex.getLocalizedMessage(), webRequest.getDescription(false)));
        return this.jsonResponseEntity(this.cleaBadRequestExceptionToApiError(ex, status));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.error(String.format(ERROR_MESSAGE_TEMPLATE, ex.getLocalizedMessage(), request.getDescription(false)));
        ApiError error = new ApiError(
                status.value(),
                Instant.now(),
                ex.getLocalizedMessage().split(":")[0],
                Set.of()
        );
        return new ResponseEntity<>(error, headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOtherException(Exception ex, WebRequest webRequest) {
        final HttpStatus status = getHttpStatus(ex);
        log.error(String.format(ERROR_MESSAGE_TEMPLATE, ex.getLocalizedMessage(), webRequest.getDescription(false)));
        return this.jsonResponseEntity(this.exceptionToApiError(ex, status));
    }

    private HttpStatus getHttpStatus(Exception ex) {
        final ResponseStatus responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        return responseStatus == null ? HttpStatus.INTERNAL_SERVER_ERROR : responseStatus.value();
    }

    private ResponseEntity<ApiError> jsonResponseEntity(ApiError apiError) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(apiError, headers, apiError.getHttpStatus());
    }

    private ApiError exceptionToApiError(Exception ex, HttpStatus status) {
        return new ApiError(
                status.value(),
                ex instanceof AbstractCleaException ? ((AbstractCleaException) ex).getTimestamp() : Instant.now(),
                ex.getLocalizedMessage(),
                Set.of()
        );
    }

    private ApiError cleaBadRequestExceptionToApiError(CleaBadRequestException ex, HttpStatus status) {
        final String splitRegex = "\\.";
        Set<ValidationError> superErrors = ex.getReportRequestViolations().stream().map(
                it -> {
                    String[] objectSplits = it.getRootBeanClass().getName().split(splitRegex);
                    String[] fieldSplits = it.getPropertyPath().toString().split(splitRegex);
                    return new ValidationError(
                            objectSplits[objectSplits.length - 1],
                            fieldSplits[fieldSplits.length - 1],
                            it.getInvalidValue(),
                            it.getMessage()
                    );
                }
        ).collect(Collectors.toSet());
        Set<ValidationError> subErrors = ex.getVisitViolations().stream().map(
                it -> {
                    String[] objectSplits = it.getRootBeanClass().getName().split(splitRegex);
                    String[] fieldSplits = it.getPropertyPath().toString().split(splitRegex);
                    return new ValidationError(
                            objectSplits[objectSplits.length - 1],
                            fieldSplits[fieldSplits.length - 1],
                            it.getInvalidValue(),
                            it.getMessage()
                    );
                }
        ).collect(Collectors.toSet());
        return new ApiError(
                status.value(),
                ex.getTimestamp(),
                ex.getLocalizedMessage(),
                Stream.concat(
                        superErrors.stream(),
                        subErrors.stream()
                ).collect(Collectors.toSet())
        );
    }
}
