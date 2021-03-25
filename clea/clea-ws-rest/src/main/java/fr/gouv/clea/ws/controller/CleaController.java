package fr.gouv.clea.ws.controller;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import fr.gouv.clea.ws.api.CleaWsRestAPI;
import fr.gouv.clea.ws.dto.ReportResponse;
import fr.gouv.clea.ws.service.IAuthorizationService;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.utils.BadArgumentsLoggerService;
import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "${controller.path.prefix}")
@Slf4j
public class CleaController implements CleaWsRestAPI {

    public static final String MALFORMED_VISIT_LOG_MESSAGE = "Filtered out %d malformed visits of %d while Exposure Status Request";
    private final IReportService reportService;
    private final IAuthorizationService authorizationService;
    private final BadArgumentsLoggerService badArgumentsLoggerService;
    private final WebRequest webRequest;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Autowired
    public CleaController(
            IReportService reportService,
            IAuthorizationService authorizationService,
            BadArgumentsLoggerService badArgumentsLoggerService,
            WebRequest webRequest,
            ObjectMapper objectMapper,
            Validator validator
    ) {
        this.reportService = reportService;
        this.authorizationService = authorizationService;
        this.badArgumentsLoggerService = badArgumentsLoggerService;
        this.webRequest = webRequest;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    @PostMapping(
            path = UriConstants.API_V1 + UriConstants.REPORT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    // TODO: Also we should switch from AuthorizationService to SpringSecurity using jwtDecoder
    public ReportResponse report(@RequestBody @Valid ReportRequest reportRequestVo) {
        String auth = webRequest.getHeader("Authorization");
        this.authorizationService.checkAuthorization(auth);
        ReportRequest filtered = this.filterReports(reportRequestVo, webRequest);
        reportService.report(filtered);
        String message = String.format("%s reports processed, %s rejected", filtered.getVisits().size(), reportRequestVo.getVisits().size() - filtered.getVisits().size());
        log.info(message);
        return new ReportResponse(true, message);
    }

    protected ReportRequest filterReports(ReportRequest report, WebRequest webRequest) {
        Set<ConstraintViolation<ReportRequest>> superViolations = validator.validate(report);
        if (!superViolations.isEmpty()) {
            this.badArgumentsLoggerService.logValidationErrorMessage(superViolations, webRequest);
            log.warn(String.format(MALFORMED_VISIT_LOG_MESSAGE, report.getVisits().size(), report.getVisits().size()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to validate request");
        } else {
            List<Visit> validVisits = report.getVisits().stream()
                    .filter(visit -> {
                        Set<ConstraintViolation<Visit>> subViolations = validator.validate(visit);
                        if (!subViolations.isEmpty()) {
                            this.badArgumentsLoggerService.logValidationErrorMessage(subViolations, webRequest);
                            return false;
                        } else {
                            return true;
                        }
                    }).collect(Collectors.toList());
            int nbVisits = report.getVisits().size();
            int nbFilteredVisits = nbVisits - validVisits.size();
            if (nbFilteredVisits > 0) {
                log.warn(String.format(MALFORMED_VISIT_LOG_MESSAGE, nbFilteredVisits, nbVisits));
            }
            return new ReportRequest(validVisits, report.getPivotDateAsNtpTimestamp());
        }
    }

    @PostConstruct
    private void disableAutomaticJsonDeserialization() {
        objectMapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) {
                return null;
            }

            @Override
            public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType, Number valueToConvert, String failureMsg) {
                return null;
            }
        });
    }

}
