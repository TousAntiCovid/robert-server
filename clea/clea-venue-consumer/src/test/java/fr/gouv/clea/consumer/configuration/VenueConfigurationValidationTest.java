package fr.gouv.clea.consumer.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

public class VenueConfigurationValidationTest {
    @Test
    void should_get_expected_values() {
        Map<String, String> properties = new HashMap<>();
        properties.put("clea.conf.durationUnitInSeconds", "1800");
        properties.put("clea.conf.driftBetweenDeviceAndOfficialTimeInSecs", "300");
        properties.put("clea.conf.cleaClockDriftInSecs", "300");
        properties.put("clea.conf.retentionDurationInDays", "14");

        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThat(config.getDurationUnitInSeconds()).isEqualTo(1800);
        assertThat(config.getDriftBetweenDeviceAndOfficialTimeInSecs()).isEqualTo(300);
        assertThat(config.getCleaClockDriftInSecs()).isEqualTo(300);
        assertThat(config.getRetentionDurationInDays()).isEqualTo(14);
    }

    @Test
    void should_get_no_exception_when_configuration_is_valid() {
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(this.getValidVenueConsumerProperties());
        this.validateConfiguration(config);
    }
    
    @Test
    void should_get_violation_when_duration_unit_not_valid() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.put("clea.conf.durationUnitInSeconds", "-1");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_missing_duration_unit() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.remove("clea.conf.durationUnitInSeconds");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_driftBetweenDeviceAndOfficialTimeInSecs_not_valid() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.put("clea.conf.driftBetweenDeviceAndOfficialTimeInSecs", "-1");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_missing_driftBetweenDeviceAndOfficialTimeInSecs() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.remove("clea.conf.driftBetweenDeviceAndOfficialTimeInSecs");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_cleaClockDriftInSecs_not_valid() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.put("clea.conf.cleaClockDriftInSecs", "-1");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_missing_cleaClockDriftInSecs() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.remove("clea.conf.cleaClockDriftInSecs");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_retentionDurationInDays_less_than_min_value() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.put("clea.conf.retentionDurationInDays", "9");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_retentionDurationInDays_greater_than_max_value() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.put("clea.conf.retentionDurationInDays", "31");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    @Test
    void should_get_violation_when_missing_retentionDurationInDays() {
        Map<String, String> properties = this.getValidVenueConsumerProperties();
        properties.remove("clea.conf.retentionDurationInDays");
        VenueConsumerConfiguration config = this.getConfigurationFromProperties(properties);
        
        assertThatExceptionOfType(ConstraintViolationException.class)
            .isThrownBy(() -> { this.validateConfiguration(config); });
    }
    
    VenueConsumerConfiguration getConfigurationFromProperties(Map<String, String> properties) {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        BindResult<VenueConsumerConfiguration> result = binder.bind("clea.conf", VenueConsumerConfiguration.class);

        // Should return true if bound successfully.
        assertThat(result.isBound()).isTrue();
        VenueConsumerConfiguration config = result.get();
        return config;
    }
    
    void validateConfiguration(VenueConsumerConfiguration config) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<VenueConsumerConfiguration>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
    
    Map<String, String> getValidVenueConsumerProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("clea.conf.durationUnitInSeconds", "1800");
        properties.put("clea.conf.driftBetweenDeviceAndOfficialTimeInSecs", "300");
        properties.put("clea.conf.cleaClockDriftInSecs", "300");
        properties.put("clea.conf.retentionDurationInDays", "14");
        return properties;
    }
}
