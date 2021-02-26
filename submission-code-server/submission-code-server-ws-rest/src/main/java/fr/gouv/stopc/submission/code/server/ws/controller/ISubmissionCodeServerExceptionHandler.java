package fr.gouv.stopc.submission.code.server.ws.controller;

import fr.gouv.stopc.submission.code.server.ws.controller.error.SubmissionCodeServerException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public interface ISubmissionCodeServerExceptionHandler {



    @ExceptionHandler(SubmissionCodeServerException.class)
    ResponseEntity<Object> handleSubmissionCodeServer(
            SubmissionCodeServerException submissionCodeServerException,
            WebRequest request
    );
}
