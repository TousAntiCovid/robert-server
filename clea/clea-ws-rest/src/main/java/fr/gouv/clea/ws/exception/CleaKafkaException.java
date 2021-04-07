package fr.gouv.clea.ws.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CleaKafkaException extends AbstractCleaException {

    private static final String EX_CODE = "clea-004";
    private static final String MESSAGE = "Could not persist request (Queue down)";

    public CleaKafkaException() {
        super(MESSAGE, EX_CODE);
    }
}
