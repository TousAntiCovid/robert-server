package fr.gouv.tacw.ws.service;

import java.util.stream.Stream;

import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.vo.ReportRequestVo;

public interface WarningService {
	public boolean getStatus(Stream<OpaqueVisit> tokens);
	public void reportVisitsWhenInfected(ReportRequestVo reportRequestVo);
}
