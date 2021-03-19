package fr.gouv.clea.ws.service;

import java.util.List;

import fr.gouv.clea.ws.dto.Visit;
import fr.gouv.clea.ws.model.DecodedLocationSpecificPart;

public interface IReportService {
    List<DecodedLocationSpecificPart> report(String jwtToken, List<Visit> visits);
}
