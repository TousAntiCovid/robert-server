package fr.gouv.stopc.robertserver.ws.exception;

import fr.gouv.stopc.robertserver.ws.utils.MessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice
@Slf4j
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            final HttpRequestMethodNotSupportedException e,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {

        final var message = e.getLocalizedMessage();
        log.error(message);

        return new ResponseEntity<>(buildApiError(message), status);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            final MethodArgumentNotValidException e,
            final HttpHeaders headers,
            final HttpStatus status,
            final WebRequest request) {

        final var message = MessageConstants.INVALID_DATA.getValue();
        log.error(e.getMessage());
        log.error(message, e.getCause());

        return new ResponseEntity<>(buildApiError(message), status);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Object> handleException(final Exception e) {

        var message = MessageConstants.ERROR_OCCURED.getValue();
        if (e instanceof RobertServerException) {
            message = e.getMessage();
        }
        log.error(e.getMessage());
        log.error(message, e.getCause());
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        log.error(errors.toString());

        return new ResponseEntity<>(buildApiError(message), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = RobertServerBadRequestException.class)
    public ResponseEntity<Object> handleBadRequestException(Exception badRequestException) {

        final var message = badRequestException.getMessage();
        log.warn(message, badRequestException);
        return new ResponseEntity<>(buildApiError(message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = RobertServerUnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(final Exception unauthorizedException) {

        final var message = unauthorizedException.getMessage();
        log.warn(message, unauthorizedException);
        return new ResponseEntity<>(buildApiError(message), HttpStatus.UNAUTHORIZED);
    }

    private ApiError buildApiError(final String message) {

        return ApiError.builder().message(message).build();
    }
}
