package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.api.CleaWsRestAPI;
import fr.gouv.clea.ws.dto.ReportResponse;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.service.IAuthorizationService;
import fr.gouv.clea.ws.service.IReportService;
import fr.gouv.clea.ws.utils.UriConstants;
import fr.gouv.clea.ws.vo.ReportRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(path = "${controller.path.prefix}")
@Slf4j
public class CleaController implements CleaWsRestAPI {

    private final IReportService reportService;

    private final IAuthorizationService authorizationService;

    private final HttpServletRequest request;

    @Autowired
    public CleaController(
            IReportService reportService,
            IAuthorizationService authorizationService,
            HttpServletRequest request
    ) {
        this.reportService = reportService;
        this.authorizationService = authorizationService;
        this.request = request;
    }

    @Override
    @PostMapping(
            path = UriConstants.API_V1 + UriConstants.REPORT,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    // TODO: Also we should switch from AuthorizationService to SpringSecurity using jwtDecoder
    public ReportResponse report(
            @RequestBody @Valid ReportRequest reportRequestVo
    ) {
        String auth = request.getHeader("Authorization");
        this.authorizationService.checkAuthorization(auth);
        List<DecodedVisit> reported = reportService.report(reportRequestVo);
        String message = String.format("%s reports processed, %s rejected", reported.size(), reportRequestVo.getVisits().size() - reported.size());
        log.info(message);
        return new ReportResponse(true, message);
    }

}
