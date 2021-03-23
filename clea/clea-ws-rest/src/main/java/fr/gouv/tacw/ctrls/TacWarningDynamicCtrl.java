package fr.gouv.tacw.ctrls;

import fr.gouv.tacw.apis.TacWarningDynamicAPI;
import fr.gouv.tacw.dtos.DecodedLocationSpecificPart;
import fr.gouv.tacw.dtos.ReportResponse;
import fr.gouv.tacw.dtos.Reports;
import fr.gouv.tacw.services.IReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class TacWarningDynamicCtrl implements TacWarningDynamicAPI {

    private final IReportService reportService;

    @Autowired
    public TacWarningDynamicCtrl(IReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    @PostMapping(
            value = "/report",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ReportResponse report(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody Reports body
    ) {
        List<DecodedLocationSpecificPart> reported = reportService.report(authorization, body);
        String message = reported.size() + " reports processed, " + (body.getReports().size() - reported.size()) + " rejected";
        log.info(message);
        return new ReportResponse(true, message);
    }
}
