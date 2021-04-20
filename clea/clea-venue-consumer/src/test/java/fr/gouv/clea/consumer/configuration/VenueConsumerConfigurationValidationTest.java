package fr.gouv.clea.consumer.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VenueConsumerConfigurationValidationTest {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        validator = VALIDATOR_FACTORY.getValidator();
    }
    
    @Test
    void should_get_no_exception_when_configuration_is_valid() {
        VenueConsumerConfiguration config = this.getValidVenueConsumerConfiguration();
        
        assertThat(validator.validate(config)).isEmpty();
    }
    
    @Test
    void should_get_violation_when_duration_unit_not_valid() {
        VenueConsumerConfiguration config = this.getValidVenueConsumerConfiguration()
                .toBuilder().durationUnitInSeconds(-1).build();
        
        assertThat(validator.validate(config)).hasSize(1);
    }
    
    @Test
    void should_get_violation_when_driftBetweenDeviceAndOfficialTimeInSecs_not_valid() {
        VenueConsumerConfiguration config = this.getValidVenueConsumerConfiguration()
                .toBuilder().driftBetweenDeviceAndOfficialTimeInSecs(-1).build();
        
        assertThat(validator.validate(config)).hasSize(1);
    }
    
    @Test
    void should_get_violation_when_cleaClockDriftInSecs_not_valid() {
        VenueConsumerConfiguration config = this.getValidVenueConsumerConfiguration()
                .toBuilder().cleaClockDriftInSecs(-1).build();
        
        assertThat(validator.validate(config)).hasSize(1);
    }
    
    @Test
    void should_get_violation_when_retentionDurationInDays_less_than_min_value() {
        VenueConsumerConfiguration config = this.getValidVenueConsumerConfiguration()
                .toBuilder().retentionDurationInDays(9).build();
        
        assertThat(validator.validate(config)).hasSize(1);
    }
    
    @Test
    void should_get_violation_when_retentionDurationInDays_greater_than_max_value() {
        VenueConsumerConfiguration config = this.getValidVenueConsumerConfiguration()
                .toBuilder().retentionDurationInDays(31).build();
        
        assertThat(validator.validate(config)).hasSize(1);
    }
    
    VenueConsumerConfiguration getValidVenueConsumerConfiguration() {
        return VenueConsumerConfiguration.builder()
            .durationUnitInSeconds(1800)
            .statSlotDurationInSeconds(1800)
            .driftBetweenDeviceAndOfficialTimeInSecs(300)
            .cleaClockDriftInSecs(300)
            .retentionDurationInDays(14)
            .build();
    }
}
