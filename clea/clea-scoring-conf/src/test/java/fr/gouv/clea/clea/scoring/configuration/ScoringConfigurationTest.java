package fr.gouv.clea.clea.scoring.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ScoringConfigurationTest {
    @Test
    void testConfigurationWithNoWildcardHasMaxPriority() {
        ExposureTimeConfiguration scoring = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring.getPriority()).isEqualTo(100);
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithNoWildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory1Wildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory2Wildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithCategory1And2Wildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }


    @Test
    void testConfigurationWithWildcardDoesNotHavePriorityOverConfigurationWithTypeAndCategory1Wildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isLessThan(scoring2.getPriority());
    }
    
    @Test
    void testConfigurationWithCategory1WithNoWildcardHasPriorityOverConfigurationWithCategory2WithNoWildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();       
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(1)
                .build();
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }
    
    @Test
    void testConfigurationWithCategory1HasPriorityOverConfigurationWithWildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithTypeHasPriorityOverConfigurationWithWildcard() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }

    @Test
    void testConfigurationWithTypeHasPriorityOverConfigurationWithWildcards() {
        ExposureTimeConfiguration scoring1 = ExposureTimeConfiguration.builder()
                .venueType(1)
                .venueCategory1(ScoringConfiguration.wildcardValue)
                .venueCategory2(ScoringConfiguration.wildcardValue)
                .build();
        ExposureTimeConfiguration scoring2 = ExposureTimeConfiguration.builder()
                .venueType(ScoringConfiguration.wildcardValue)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();       
        
        assertThat(scoring1.getPriority()).isGreaterThan(scoring2.getPriority());
    }
}
