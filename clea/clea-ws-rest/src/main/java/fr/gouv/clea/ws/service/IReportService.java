package fr.gouv.clea.ws.service;

import java.util.List;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.vo.ReportRequest;

public interface IReportService {
    List<DecodedVisit> report(String jwtToken, ReportRequest reportRequestVo);
}
