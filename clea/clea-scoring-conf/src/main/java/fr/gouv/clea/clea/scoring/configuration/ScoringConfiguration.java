package fr.gouv.clea.clea.scoring.configuration;

import java.util.Comparator;
import java.util.List;

public abstract class ScoringConfiguration {
    
    abstract public List<? extends ScoringConfigurationItem> getScorings();

    public ScoringConfigurationItem getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        return this.getScorings().stream()
            .filter(scoring -> scoring.isCompatibleWith(venueType, venueCategory1, venueCategory2))
            .max(Comparator.comparing(ScoringConfigurationItem::getPriority))
            .get();
    }
}
