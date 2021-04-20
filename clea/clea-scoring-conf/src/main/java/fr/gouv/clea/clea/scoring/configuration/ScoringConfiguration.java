package fr.gouv.clea.clea.scoring.configuration;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class ScoringConfiguration {

    abstract public List<? extends ScoringConfigurationItem> getScorings();

    public ScoringConfigurationItem getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        log.debug("Fetching rules candidates for venueType : {}, venueCategory1 : {}, venuCategory2: {}", venueType, venueCategory1, venueCategory2);
        List<ScoringConfigurationItem> compatibleRules = this.getScorings().stream()
                .filter(scoring -> scoring.isCompatibleWith(venueType, venueCategory1, venueCategory2))
                .collect(Collectors.toList());

        log.debug("Found {} compatibles rules", compatibleRules.size());
        ScoringConfigurationItem selectedRule = null;

        if (compatibleRules.size() == 1) {
            log.info("Found suitable rule {}", compatibleRules.get(0));
            return compatibleRules.get(0);
        }

        for (ScoringConfigurationItem rule : compatibleRules) {
            if (rule.isFullMatch()) {
                log.info("Found matching rule {}", rule);
                selectedRule = rule;
                break;
            } else if (rule.getVenueType() != ScoringConfigurationItem.wildcardValue &&
                    (
                            rule.getVenueCategory1() == ScoringConfigurationItem.wildcardValue ||
                                    rule.getVenueCategory2() == ScoringConfigurationItem.wildcardValue
                    )
            ) {
                if (rule.getVenueCategory1() != ScoringConfigurationItem.wildcardValue) {
                    log.info("Found suitable rule {}", rule);
                    selectedRule = rule;
                    break;
                }
            } else if (rule.getVenueType() == ScoringConfigurationItem.wildcardValue &&
                    (
                            rule.getVenueCategory1() == ScoringConfigurationItem.wildcardValue ||
                                    rule.getVenueCategory2() == ScoringConfigurationItem.wildcardValue
                    )
            ) {
                if (rule.getVenueCategory1() != ScoringConfigurationItem.wildcardValue) {
                    log.info("Found suitable rule {}", rule);
                    selectedRule = rule;
                    break;
                }
            }
        }

        return selectedRule;

    }

}
