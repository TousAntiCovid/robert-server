package fr.gouv.tacw.ctrls;

import fr.gouv.tacw.apis.TacWarningDynamicAPI;
import fr.gouv.tacw.data.DecodedLocationSpecificPart;
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
            @RequestHeader(value = "Authorization", required = false) String jwtToken,
            @RequestBody Reports body
    ) {
        List<DecodedLocationSpecificPart> reported = reportService.report(jwtToken, body);
        String message = body.getReports().size() - reported.size() + " reports processed, " + reported.size() + " rejected";
        log.info(message);
        return new ReportResponse(true, message);
    }
}
