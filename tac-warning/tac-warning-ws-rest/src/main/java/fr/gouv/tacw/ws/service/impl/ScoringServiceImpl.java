package fr.gouv.tacw.ws.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.service.ScoringService;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.vo.VenueTypeVo;

@Service
public class ScoringServiceImpl implements ScoringService {
    private final TacWarningWsRestConfiguration configuration;

    public ScoringServiceImpl(TacWarningWsRestConfiguration configuration) {
        super();
        this.configuration = configuration;
    }
    
    @Override
    public long getScoreIncrement(VenueTypeVo venueType) {
        Map<String, Integer> venueTypePeopleThresholds = this.configuration.getVenueTypePeopleThreshold();
        int peopleThreshold = venueTypePeopleThresholds.getOrDefault(venueType.toString(), venueTypePeopleThresholds.get("default"));
        return Math.round(configuration.getScoreThreshold() / peopleThreshold);
    }

}
