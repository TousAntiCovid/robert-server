package fr.gouv.clea.ws.exception;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@NoArgsConstructor
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CleaUnauthorizedException extends RuntimeException {
    private static final long serialVersionUID = -2742936878446030656L;

    public CleaUnauthorizedException(String message) {
        super(message);
    }
}
