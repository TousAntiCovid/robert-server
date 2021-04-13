package fr.gouv.clea.clea.scoring.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ScoringConfigurationItemTest {
    @Test
    void testConfigurationWithNoWildcardHasMaxPriority() {
        ExposureTimeConfigurationItem scoring = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring.getPriority()).isEqualTo(100);
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithNoWildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory1Wildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory2Wildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory1And2Wildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }


    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithTypeAndCategory1Wildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }
    
    @Test
    void testConfigurationWithCategory1WithNoWildcardHasPriorityOverConfigurationWithCategory2WithNoWildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();       
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }
    
    @Test
    void testConfigurationWithCategory1HasPriorityOverConfigurationWithWildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithTypeHasPriorityOverConfigurationWithWildcard() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithTypeHasPriorityOverConfigurationWithWildcards() {
        ExposureTimeConfigurationItem scoring1 = ExposureTimeConfigurationItem.builder()
                .venueType(1)
                .venueCategory1(ScoringConfigurationItem.wildcardValue)
                .venueCategory2(ScoringConfigurationItem.wildcardValue)
                .build();
        ExposureTimeConfigurationItem scoring2 = ExposureTimeConfigurationItem.builder()
                .venueType(ScoringConfigurationItem.wildcardValue)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }
}
