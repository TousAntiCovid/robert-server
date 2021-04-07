package fr.gouv.clea.ws.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CleaForbiddenException extends AbstractCleaException {

    private static final String EX_CODE = "clea-001";
    private static final String MESSAGE = "Could not be authenticated (Authorisation header/token invalid)";

    public CleaForbiddenException() {
        super(MESSAGE, EX_CODE);
    }
}
