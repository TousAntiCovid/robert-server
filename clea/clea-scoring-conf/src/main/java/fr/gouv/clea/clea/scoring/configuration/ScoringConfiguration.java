package fr.gouv.clea.clea.scoring.configuration;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class ScoringConfiguration {

    public abstract List<? extends ScoringConfigurationItem> getScorings();

    public ScoringConfigurationItem getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        log.info("Fetching rules candidates for venueType : {}, venueCategory1 : {}, venuCategory2: {}", venueType, venueCategory1, venueCategory2);
        List<ScoringConfigurationItem> compatibleRules = this.getScorings().stream()
                .filter(scoring -> scoring.isCompatibleWith(venueType, venueCategory1, venueCategory2))
                .collect(Collectors.toList());

        log.info("Found {} compatibles rules", compatibleRules.size());

        //Only one match found
        if (compatibleRules.size() == 1) {
            log.info("Found suitable rule {}", compatibleRules.get(0));
            return compatibleRules.get(0);
        }

        //All are wildcarded, then rule to apply is the default one
        if (allAreWilcarded(venueType, venueCategory1, venueCategory2)) {
            getConfigurationFor(
                    ScoringConfigurationItem.wildcardValue,
                    ScoringConfigurationItem.wildcardValue,
                    ScoringConfigurationItem.wildcardValue);
        }

        ScoringConfigurationItem bestRuleCandidate = compatibleRules.get(0);
        for (ScoringConfigurationItem rule : compatibleRules) {
            if (rule.isFullMatch()) {
                log.info("Found matching rule {}", rule);
                return rule;
            } else if (bothCategoryAreWildcard(bestRuleCandidate) && firstCategoryIsNotWildcarded(rule)) {
                bestRuleCandidate = rule;
            } else if (eitherOneCategoryIsWildcard(bestRuleCandidate) && firstCategoryIsNotWildcarded(rule)) {
                bestRuleCandidate = rule;
            } else if (firstCategoryIsNotWildcarded(bestRuleCandidate) && secondCategoryIsNotWildcarded(bestRuleCandidate)) {
                return bestRuleCandidate;
            }
        }
        log.info("Retrieving best rule {}", bestRuleCandidate);
        return bestRuleCandidate;

    }

    private boolean allAreWilcarded(int venueType, int venueCategory1, int venueCategory2) {
        int count = 0;
        if (venueType == ScoringConfigurationItem.wildcardValue) {
            count++;
        }
        if (venueCategory1 == ScoringConfigurationItem.wildcardValue) {
            count++;
        }
        if (venueCategory2 == ScoringConfigurationItem.wildcardValue) {
            count++;
        }
        return count == 3;
    }


    private boolean eitherOneCategoryIsWildcard(ScoringConfigurationItem rule) {
        return isWildcarded(rule.getVenueCategory1()) || isWildcarded(rule.getVenueCategory2());
    }

    private boolean bothCategoryAreWildcard(ScoringConfigurationItem rule) {
        return isWildcarded(rule.getVenueCategory1()) && isWildcarded(rule.getVenueCategory2());
    }

    private boolean isWildcarded(int venueItem) {
        return venueItem == ScoringConfigurationItem.wildcardValue;
    }

    private boolean firstCategoryIsNotWildcarded(ScoringConfigurationItem rule) {
        return isWildcarded(rule.getVenueCategory1());
    }

    private boolean secondCategoryIsNotWildcarded(ScoringConfigurationItem rule) {
        return isWildcarded(rule.getVenueCategory2());
    }

}
