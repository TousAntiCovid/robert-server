package fr.gouv.clea.ws.service;

import java.util.List;

import fr.gouv.clea.ws.dto.Visit;
import fr.gouv.clea.ws.model.DecodedVisit;

public interface IReportService {
    List<DecodedVisit> report(String jwtToken, List<Visit> visits);
}
