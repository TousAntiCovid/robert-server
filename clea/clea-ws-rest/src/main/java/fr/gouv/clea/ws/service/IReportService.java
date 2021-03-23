package fr.gouv.clea.ws.service;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.vo.ReportRequest;

import java.util.List;

public interface IReportService {
    List<DecodedVisit> report(String jwtToken, ReportRequest reportRequestVo);
}
