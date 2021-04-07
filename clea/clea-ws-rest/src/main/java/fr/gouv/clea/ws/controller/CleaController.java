package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.api.CleaWsRestAPI;
import fr.gouv.clea.ws.dto.ReportResponse;
import fr.gouv.clea.ws.exception.CleaBadRequestException;
import fr.gouv.clea.ws.service.IAuthorizationService;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.utils.BadArgumentsLoggerService;
import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.HashSet;
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
    private final Validator validator;

    @Autowired
    public CleaController(
            IReportService reportService,
            IAuthorizationService authorizationService,
            BadArgumentsLoggerService badArgumentsLoggerService,
            WebRequest webRequest,
            Validator validator
    ) {
        this.reportService = reportService;
        this.authorizationService = authorizationService;
        this.badArgumentsLoggerService = badArgumentsLoggerService;
        this.webRequest = webRequest;
        this.validator = validator;
    }

    @Override
    @PostMapping(
            path = UriConstants.API_V1 + UriConstants.REPORT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    // TODO: Also we should switch from AuthorizationService to SpringSecurity using jwtDecoder
    public ReportResponse report(@RequestBody ReportRequest reportRequestVo) {
        String auth = webRequest.getHeader("Authorization");
        this.authorizationService.checkAuthorization(auth);
        ReportRequest filtered = this.filterReports(reportRequestVo, webRequest);
        reportService.report(filtered);
        String message = String.format("%s reports processed, %s rejected", filtered.getVisits().size(), reportRequestVo.getVisits().size() - filtered.getVisits().size());
        log.info(message);
        return new ReportResponse(true, message);
    }

    private ReportRequest filterReports(ReportRequest report, WebRequest webRequest) {
        Set<ConstraintViolation<ReportRequest>> superViolations = validator.validate(report);
        if (!superViolations.isEmpty()) {
            throw new CleaBadRequestException(superViolations, Set.of());
        } else {
            Set<ConstraintViolation<Visit>> subViolations = new HashSet<>();
            List<Visit> validVisits = report.getVisits().stream()
                    .filter(
                            visit -> {
                                subViolations.addAll(validator.validate(visit));
                                if (!subViolations.isEmpty()) {
                                    this.badArgumentsLoggerService.logValidationErrorMessage(subViolations, webRequest);
                                    return false;
                                } else {
                                    return true;
                                }
                            }
                    ).collect(Collectors.toList());
            if (validVisits.isEmpty()) {
                throw new CleaBadRequestException(Set.of(), subViolations);
            }
            int totalSize = report.getVisits().size();
            int filteredSize = totalSize - validVisits.size();
            if (filteredSize > 0) {
                log.warn(String.format(MALFORMED_VISIT_LOG_MESSAGE, filteredSize, totalSize));
            }
            return new ReportRequest(validVisits, report.getPivotDateAsNtpTimestamp());
        }
    }
}
