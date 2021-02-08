package fr.gouv.tacw.ws.service;

import java.util.List;
import java.util.stream.Stream;

import fr.gouv.tacw.database.model.ScoreResult;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.vo.VisitVo;

public interface WarningService {
    public ScoreResult getStatus(Stream<OpaqueVisit> tokens);
	public void reportVisitsWhenInfected(List<VisitVo> visits);
}
