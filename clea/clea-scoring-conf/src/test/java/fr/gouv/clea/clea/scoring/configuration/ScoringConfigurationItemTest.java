package fr.gouv.clea.clea.scoring.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ScoringConfigurationItemTest {
    @Test
    void testConfigurationWithNoWildcardHasMaxPriority() {
        ScoringConfigurationItem scoring = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring.getPriority()).isEqualTo(100);
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithNoWildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory1Wildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory2Wildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory1And2Wildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }


    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithTypeAndCategory1Wildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }
    
    @Test
    void testConfigurationWithCategory1WithNoWildcardHasPriorityOverConfigurationWithCategory2WithNoWildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();       
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }
    
    @Test
    void testConfigurationWithCategory1HasPriorityOverConfigurationWithWildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithTypeHasPriorityOverConfigurationWithWildcard() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithTypeHasPriorityOverConfigurationWithWildcards() {
        ScoringConfigurationItem scoring1 = ScoringConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ScoringConfigurationItem scoring2 = ScoringConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }
}
