package fr.gouv.clea.ws.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractCleaException extends RuntimeException {
    protected String code;
    protected Instant timestamp;

    protected AbstractCleaException(String message, String code) {
        super(message);
        this.code = code;
        this.timestamp = Instant.now();
    }
}
