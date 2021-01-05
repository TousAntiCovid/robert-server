package fr.gouv.tacw.ws.service;

import java.util.List;
import java.util.stream.Stream;

import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.model.RiskLevel;
import fr.gouv.tacw.ws.vo.VisitVo;

public interface WarningService {
    public RiskLevel getStatus(Stream<OpaqueVisit> tokens);
	public void reportVisitsWhenInfected(List<VisitVo> visits);
}
