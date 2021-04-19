package fr.gouv.tac.analytics.server.controller;

import java.time.ZonedDateTime;

import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;
import fr.gouv.tac.analytics.server.controller.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    public static final String PAYLOAD_TOO_LARGE = "[PAYLOAD TOO LARGE]";

    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<ErrorVo> exception(final AuthenticationException e) {
        return errorVoBuilder(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<ErrorVo> exception(final ConstraintViolationException e) {

        if (e.getMessage().contains(PAYLOAD_TOO_LARGE)) {
            // log dedicated to raised an alarm from supervision
            log.error("Too large payload has been received", e);
            return errorVoBuilder(e, HttpStatus.PAYLOAD_TOO_LARGE);
        } else {
            return errorVoBuilder(e, HttpStatus.BAD_REQUEST);
        }
    }

    @ExceptionHandler(value = JsonProcessingException.class)
    public ResponseEntity<ErrorVo> exception(final JsonProcessingException e) {
        return errorVoBuilder(e, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorVo> exception(final Exception e) {
        log.warn("Unexpected error :", e);
        return errorVoBuilder(e, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorVo> errorVoBuilder(final Exception e, final HttpStatus httpStatus) {
        final ErrorVo errorVo = ErrorVo.builder()
                .message(e.getMessage())
                .timestamp(ZonedDateTime.now())
                .build();
        return ResponseEntity.status(httpStatus).body(errorVo);
    }

}
