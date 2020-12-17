package fr.gouv.tacw.service;

import fr.gouv.tacw.ws.vo.VenueTypeVo;

public interface ScoringService {

    long getScoreIncrement(VenueTypeVo venueTypeVo);

}
