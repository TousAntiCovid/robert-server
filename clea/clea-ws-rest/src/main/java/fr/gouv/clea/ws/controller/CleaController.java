package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.api.CleaWsRestAPI;
import fr.gouv.clea.ws.dto.ReportResponse;
import fr.gouv.clea.ws.exception.CleaBadRequestException;
import fr.gouv.clea.ws.model.DecodedVisit;
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
    private final BadArgumentsLoggerService badArgumentsLoggerService;
    private final WebRequest webRequest;
    private final Validator validator;

    @Autowired
    public CleaController(
            IReportService reportService,
            BadArgumentsLoggerService badArgumentsLoggerService,
            WebRequest webRequest,
            Validator validator
    ) {
        this.reportService = reportService;
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
    public ReportResponse report(@RequestBody ReportRequest reportRequestVo) {
        ReportRequest filtered = this.filterReports(reportRequestVo, webRequest);
        List<DecodedVisit> reported = List.of();
        if (!filtered.getVisits().isEmpty()) {
            reported = reportService.report(filtered);
        }
        String message = String.format("%s reports processed, %s rejected", reported.size(), reportRequestVo.getVisits().size() - reported.size());
        log.info(message);
        return new ReportResponse(true, message);
    }

    private ReportRequest filterReports(ReportRequest report, WebRequest webRequest) {
        Set<ConstraintViolation<ReportRequest>> reportRequestViolations = validator.validate(report);
        if (!reportRequestViolations.isEmpty()) {
            throw new CleaBadRequestException(reportRequestViolations, Set.of());
        } else {
            Set<ConstraintViolation<Visit>> visitViolations = new HashSet<>();
            List<Visit> validVisits = report.getVisits().stream()
                    .filter(
                            visit -> {
                                visitViolations.addAll(validator.validate(visit));
                                if (!visitViolations.isEmpty()) {
                                    this.badArgumentsLoggerService.logValidationErrorMessage(visitViolations, webRequest);
                                    return false;
                                } else {
                                    return true;
                                }
                            }
                    ).collect(Collectors.toList());
            if (validVisits.isEmpty()) {
                throw new CleaBadRequestException(Set.of(), visitViolations);
            }
            int nbVisits = report.getVisits().size();
            int nbFilteredVisits = nbVisits - validVisits.size();
            if (nbFilteredVisits > 0) {
                log.warn(String.format(MALFORMED_VISIT_LOG_MESSAGE, nbFilteredVisits, nbVisits));
            }
            return new ReportRequest(validVisits, report.getPivotDateAsNtpTimestamp());
        }
    }
}
