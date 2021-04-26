package fr.gouv.clea.ws.exception;

import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CleaBadRequestException extends AbstractCleaException {

    private static final String EX_CODE = "clea-003";
    private static final String MESSAGE = "Invalid request";

    private Set<ConstraintViolation<ReportRequest>> reportRequestViolations;
    private Set<ConstraintViolation<Visit>> visitViolations;

    public CleaBadRequestException(
            Set<ConstraintViolation<ReportRequest>> reportRequestViolations,
            Set<ConstraintViolation<Visit>> visitViolations
    ) {
        super(MESSAGE, EX_CODE);
        this.reportRequestViolations = reportRequestViolations;
        this.visitViolations = visitViolations;
    }
}
