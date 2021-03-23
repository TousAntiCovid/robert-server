package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.api.CleaWsRestAPI;
import fr.gouv.clea.ws.dto.ReportResponse;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "${controller.path.prefix}")
@Slf4j
public class CleaController implements CleaWsRestAPI {

    private final IReportService reportService;

    @Autowired
    public CleaController(IReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    @PostMapping(
            path = UriConstants.API_V1 + UriConstants.REPORT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    // TODO: Authorization should be mandatory.
    // TODO: Also we should switch from AuthorizationService to SpringSecurity using jwtDecoder
    public ReportResponse report(
            @RequestHeader(value = "Authorization", required = false) String jwtToken,
            @RequestBody @Valid ReportRequest reportRequestVo
    ) {
        List<DecodedVisit> reported = reportService.report(jwtToken, reportRequestVo);
        String message = String.format("%s reports processed, %s rejected", reported.size(), reportRequestVo.getVisits().size() - reported.size());
        log.info(message);
        return new ReportResponse(true, message);
    }

}
