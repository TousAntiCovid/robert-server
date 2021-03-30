package fr.gouv.tousantic.analytics.server.controller;

import fr.gouv.tousantic.analytics.server.controller.vo.ErrorVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.ZonedDateTime;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {


    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<ErrorVo> exception(final AuthenticationException e) {
        return errorVoBuilder(e, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorVo> exception(final MethodArgumentNotValidException e) {
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
