package fr.gouv.clea.clea.scoring.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import fr.gouv.clea.scoring.configuration.ScoringRuleComparator;
import fr.gouv.clea.scoring.configuration.risk.RiskConfigurationConverter;
import fr.gouv.clea.scoring.configuration.risk.RiskRule;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ScoringRuleComparatorTest {
        final static RiskConfigurationConverter converter = new RiskConfigurationConverter();
        
        @Test
        void should_return_lower_priority_rules_first() {
            final List<String> rawRules = List.of( 
                    "3,*,*,3,1,3.0,2.0",
                    "3,1,*,3,1,3.0,2.0",
                    "*,*,*,3,1,3.0,2.0",
                    "3,*,2,3,1,3.0,2.0",
                    "3,1,2,3,1,3.0,2.0" 
                );
            final List<RiskRule> rules = rawRules.stream().map(converter::convert).collect(Collectors.toList());
            
            Collections.sort(rules, new ScoringRuleComparator());
            
            List<String> expectedSortedRules = List.of( 
                    "*,*,*,3,1,3.0,2.0",
                    "3,*,*,3,1,3.0,2.0",
                    "3,*,2,3,1,3.0,2.0",
                    "3,1,*,3,1,3.0,2.0",
                    "3,1,2,3,1,3.0,2.0" 
                );
            assertThat(rules).containsExactlyElementsOf(expectedSortedRules.stream().map(converter::convert).collect(Collectors.toList()));
        }
}
