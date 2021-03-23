package fr.gouv.tacw.services;

import fr.gouv.tacw.dtos.DecodedLocationSpecificPart;
import fr.gouv.tacw.dtos.Reports;

import java.util.List;

public interface IReportService {

    List<DecodedLocationSpecificPart> report(String jwtToken, Reports reports);
}
