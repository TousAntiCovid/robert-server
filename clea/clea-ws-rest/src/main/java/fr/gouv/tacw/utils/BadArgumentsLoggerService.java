package fr.gouv.tacw.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolation;
import java.util.Set;

@Slf4j
@Service
public class BadArgumentsLoggerService {
    public static final String INVALID_INPUT_TEMPLATE = "Invalid input data: %s, requested uri: %s";

    public <T> void logValidationErrorMessage(Set<ConstraintViolation<T>> violations, WebRequest webRequest) {
        final String path = webRequest.getDescription(false);
        String message = violations.toString();
        log.error(String.format(INVALID_INPUT_TEMPLATE, message, path));
    }

    public void logValidationErrorMessage(BindingResult bindingResult, WebRequest webRequest) {
        final String path = webRequest.getDescription(false);
        String message = bindingResult.toString();
        log.error(String.format(INVALID_INPUT_TEMPLATE, message, path));
    }
}
