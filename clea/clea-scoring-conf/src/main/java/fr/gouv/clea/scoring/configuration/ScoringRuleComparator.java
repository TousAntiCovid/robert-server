package fr.gouv.clea.scoring.configuration;

import java.util.Comparator;

/**
 * I compare rules MATCHING a given tuple (venue type, venue categrory1, venue category 2).
 * I cannot be used to compare rules not mathcing the same tuple.
 */
public class ScoringRuleComparator implements Comparator<ScoringRule> {

    @Override
    public int compare(ScoringRule rule1, ScoringRule rule2) {
        if (rule1.isDefaultRule()) {
            return -1;
        }
        if (rule1.isFullMatch()) {
            return 1;
        }
        // No check on venue type : Wildcard on venue type is not allowed except for the default rule
        
        if (rule1.isCategory1Wildcarded() && !rule2.isCategory1Wildcarded()) {
            return -1;
        }
        if (!rule1.isCategory1Wildcarded() && rule2.isCategory1Wildcarded()) {
            return 1;
        }
        if (rule1.isCategory2Wildcarded() && !rule2.isCategory2Wildcarded()) {
            return -1;
        }
        if (!rule1.isCategory2Wildcarded() && rule2.isCategory2Wildcarded()) {
            return 1;
        }
        return 0;
    }

}
