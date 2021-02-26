package fr.gouv.stopc.submission.code.server.ws.controller.impl;

import fr.gouv.stopc.submission.code.server.ws.controller.ISubmissionCodeServerExceptionHandler;
import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
public class SubmissionCodeServerExceptionHandler extends ResponseEntityExceptionHandler implements ISubmissionCodeServerExceptionHandler {


    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request)
    {
        return this.handleSubmissionCodeServer(
                new SubmissionCodeServerException(
                        SubmissionCodeServerException.ExceptionEnum.VALIDATION_FIELD_ERROR, ex
                ),
                request);
    }

    @Override
    public ResponseEntity<Object> handleBindException(
            BindException ex,
            HttpHeaders headers, HttpStatus status, WebRequest request)
    {
        return this.handleSubmissionCodeServer(
                new SubmissionCodeServerException(
                        SubmissionCodeServerException.ExceptionEnum.VALIDATION_FIELD_ERROR, ex
                ),
                request);
    }

    @Override
    public ResponseEntity<Object> handleSubmissionCodeServer(
            SubmissionCodeServerException submissionCodeServerException,
            WebRequest request
    )
    {
        log.error("handleSubmissionCodeServer", submissionCodeServerException);
        switch (submissionCodeServerException.getServerExceptionEnum()) {
            case INVALID_CODE_TYPE_ERROR:
            case VALIDATION_FIELD_ERROR:
                return super.handleExceptionInternal(
                        submissionCodeServerException, submissionCodeServerException.body(),
                        new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

            case DB_INVALID_PARAMETERS_ERROR:
            case DB_NO_RECORD_FOR_LOT_IDENTIFIER_ERROR:
                return super.handleExceptionInternal(
                        submissionCodeServerException, submissionCodeServerException.body(),
                        new HttpHeaders(), HttpStatus.NOT_FOUND, request);

            case CODE_GENERATION_FAILED_ERROR:
            case NUMBER_OF_TRIES_REACHED_ERROR:
                return super.handleExceptionInternal(
                        submissionCodeServerException, submissionCodeServerException.body(),
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

            default:
                return this.unHandledException(submissionCodeServerException, request);
        }

    }

    ResponseEntity<Object> unHandledException(
            SubmissionCodeServerException submissionCodeServerException,
            WebRequest request
    )
    {
        final UUID uuid = UUID.randomUUID();
        final OffsetDateTime now = OffsetDateTime.now();

        String loggedCodeError = String.format("[%s%s]", now, uuid);

        log.error(loggedCodeError, submissionCodeServerException);

        return super.handleExceptionInternal(
                submissionCodeServerException, submissionCodeServerException.body(loggedCodeError),
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            @Nullable Object body,
            HttpHeaders headers, HttpStatus status,
            WebRequest request) {
        return this.handleSubmissionCodeServer(
                new SubmissionCodeServerException(
                        SubmissionCodeServerException.ExceptionEnum.UNQUALIFIED_ERROR, ex
                ), request
        );
    }
}
