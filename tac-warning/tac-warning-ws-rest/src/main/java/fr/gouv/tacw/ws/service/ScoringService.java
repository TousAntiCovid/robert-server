package fr.gouv.tacw.ws.service;

import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.ws.vo.VenueTypeVo;

public interface ScoringService {

    long getScoreIncrement(VenueTypeVo venueTypeVo);

    RiskLevel getVenueRiskLevel(VenueTypeVo venueTypeVo);

}
