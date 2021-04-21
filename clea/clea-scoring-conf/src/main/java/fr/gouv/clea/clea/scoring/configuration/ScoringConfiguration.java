package fr.gouv.clea.clea.scoring.configuration;

import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ScoringConfiguration {

    public abstract List<? extends ScoringRule> getScorings();

    public ScoringRule getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        log.debug("Fetching rules candidates for venueType : {}, venueCategory1 : {}, venuCategory2: {}", venueType, venueCategory1, venueCategory2);
        Optional<? extends ScoringRule> matchingRule = this.getScorings().stream()
                .filter(scoring -> scoring.isCompatibleWith(venueType, venueCategory1, venueCategory2))
                .max(new ScoringRuleComparator());
        if (matchingRule.isPresent()) {
            log.debug("Found matching rulefor venueType : {}, venueCategory1 : {}, venuCategory2: {}", matchingRule.get(), venueType, venueCategory1, venueCategory2);
            return matchingRule.get();
        } else {
            log.error("No scoring matching found for venueType : {}, venueCategory1 : {}, venuCategory2: {}", venueType, venueCategory1, venueCategory2);
            return null;
        }
    }

}
