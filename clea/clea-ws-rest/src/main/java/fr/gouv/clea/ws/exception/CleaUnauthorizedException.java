package fr.gouv.clea.ws.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Data
@EqualsAndHashCode(callSuper = false)
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class CleaUnauthorizedException extends AbstractCleaException {

    private static final String EX_CODE = "clea-002";
    private static final String MESSAGE = "Could not be authorized (Missing authorisation header/token)";

    public CleaUnauthorizedException() {
        super(MESSAGE, EX_CODE);
    }
}
