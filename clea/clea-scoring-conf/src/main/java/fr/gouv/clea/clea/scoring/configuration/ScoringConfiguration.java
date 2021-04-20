package fr.gouv.clea.clea.scoring.configuration;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class ScoringConfiguration {

    public abstract List<? extends ScoringRule> getScorings();

    public ScoringRule getConfigurationFor(int venueType, int venueCategory1, int venueCategory2) {
        log.debug("Fetching rules candidates for venueType : {}, venueCategory1 : {}, venuCategory2: {}", venueType, venueCategory1, venueCategory2);
        List<ScoringRule> compatibleRules = this.getScorings().stream()
                .filter(scoring -> scoring.isCompatibleWith(venueType, venueCategory1, venueCategory2))
                .collect(Collectors.toList());

        log.debug("Found {} compatibles rules", compatibleRules.size());

        //Only one match found
        if (compatibleRules.size() == 1) {
            log.debug("Found suitable rule {}", compatibleRules.get(0));
            return compatibleRules.get(0);
        }

        //All are wildcarded, then rule to apply is the default one
        if (allAreWilcarded(venueType, venueCategory1, venueCategory2)) {
            getConfigurationFor(
                    ScoringRule.wildcardValue,
                    ScoringRule.wildcardValue,
                    ScoringRule.wildcardValue);
        }

        ScoringRule bestRuleCandidate = compatibleRules.get(0);
        for (ScoringRule rule : compatibleRules) {
            if (rule.isFullMatch()) {
                log.debug("Found full matching rule {}", rule);
                return rule;
            } else if (bothCategoryAreWildcard(bestRuleCandidate) && firstCategoryIsNotWildcarded(rule)) {
                bestRuleCandidate = rule;
            } else if (eitherOneCategoryIsWildcard(bestRuleCandidate) && firstCategoryIsNotWildcarded(rule)) {
                bestRuleCandidate = rule;
            } else if (firstCategoryIsNotWildcarded(bestRuleCandidate) && secondCategoryIsNotWildcarded(bestRuleCandidate)) {
                bestRuleCandidate = rule;
            }
        }
        log.info("Retrieving best rule {} for venueType : {}, venueCategory1 : {}, venuCategory2: {}", bestRuleCandidate, venueType, venueCategory1, venueCategory2);
        return bestRuleCandidate;

    }

    private boolean allAreWilcarded(int venueType, int venueCategory1, int venueCategory2) {
        int count = 0;
        if (venueType == ScoringRule.wildcardValue) {
            count++;
        }
        if (venueCategory1 == ScoringRule.wildcardValue) {
            count++;
        }
        if (venueCategory2 == ScoringRule.wildcardValue) {
            count++;
        }
        return count == 3;
    }


    private boolean eitherOneCategoryIsWildcard(ScoringRule rule) {
        return isWildcarded(rule.getVenueCategory1()) || isWildcarded(rule.getVenueCategory2());
    }

    private boolean bothCategoryAreWildcard(ScoringRule rule) {
        return isWildcarded(rule.getVenueCategory1()) && isWildcarded(rule.getVenueCategory2());
    }

    private boolean isWildcarded(int venueItem) {
        return venueItem == ScoringRule.wildcardValue;
    }

    private boolean firstCategoryIsNotWildcarded(ScoringRule rule) {
        return isWildcarded(rule.getVenueCategory1());
    }

    private boolean secondCategoryIsNotWildcarded(ScoringRule rule) {
        return isWildcarded(rule.getVenueCategory2());
    }

}
