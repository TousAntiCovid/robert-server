package fr.gouv.tacw.ws.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class CustomRestExceptionHandler extends ResponseEntityExceptionHandler {

    public static final String ERROR_MESSAGE_TEMPLATE = "%s, requested uri: %s";
    public static final String INVALID_INPUT_TEMPLATE = "Invalid input data: %s, requested uri: %s";

    /**
     * A general handler for all uncaught exceptions
     * @throws Exception 
     */
    @ExceptionHandler({ Exception.class })
    public ResponseEntity<Object> handleAllExceptions(Exception exception, WebRequest webRequest) throws Exception {
        String message = exception.getLocalizedMessage();
        if (message == null)
            message = exception.toString();
        final String path = webRequest.getDescription(false);
        ResponseStatus responseStatus = exception.getClass().getAnnotation(ResponseStatus.class);
        final HttpStatus status = responseStatus != null ? responseStatus.value() : HttpStatus.INTERNAL_SERVER_ERROR;
        log.error(String.format(ERROR_MESSAGE_TEMPLATE, message, path), exception);
        return this.newResponseEntity(null, status);
    }

    /**
     * Handle BAD REQUEST exceptions
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatus status, WebRequest webRequest) {
        String message = "";
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            message += String.format("%s %s\n", error.getField(), error.getDefaultMessage());
        }
        for (ObjectError error : exception.getBindingResult().getGlobalErrors()) {
            message += String.format("%s %s\n", error.getObjectName(), error.getDefaultMessage());
        }
        if (exception.getBindingResult().getSuppressedFields().length > 0)
            message += "Suppressed fields: " + String.join(", ", exception.getBindingResult().getSuppressedFields());

        final String path = webRequest.getDescription(false);
        log.error(String.format(INVALID_INPUT_TEMPLATE, message, path), exception.getCause());
        return this.newResponseEntity(null, status);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request) {
        log.info("Bad Request: {}", ex.getMessage());
        log.debug("Bad Request: ", ex);

        return this.newResponseEntity(null, HttpStatus.BAD_REQUEST);
    }

    protected ResponseEntity<Object> newResponseEntity(Object body, HttpStatus status) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(null, headers, status);
    }
}
