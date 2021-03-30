package fr.gouv.tousantic.analytics.server.config.validation.validator;

import fr.gouv.tousantic.analytics.server.config.validation.ValidationParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.ConstraintValidatorContext;
import java.util.Map;

@ExtendWith(SpringExtension.class)
public class AnalyticsMongoVoInfoSizeValidatorTest {

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidationParameters validationParameters;

    @InjectMocks
    private AnalyticsVoInfoSizeValidator analyticsVoInfoSizeValidator;

    private ValidationParameters.InfoValidationParameters infoValidationParameters;

    @BeforeEach
    public void setUp() {
        infoValidationParameters = ValidationParameters.InfoValidationParameters.builder()
                .maxInfoAllowed(3)
                .maxInfoKeyLength(10)
                .maxInfoValueLength(20)
                .build();
        Mockito.when(validationParameters.getInformation()).thenReturn(infoValidationParameters);
    }

    @Test
    public void shouldAcceptNullMap() {
        final Map<String, String> info = null;

        final boolean result = this.analyticsVoInfoSizeValidator.isValid(info, constraintValidatorContext);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldAcceptMapWithLessThanMaxAllowedElement() {
        final Map<String, String> info = Map.of("key1", "value1", "key2", "value2");

        final boolean result = this.analyticsVoInfoSizeValidator.isValid(info, constraintValidatorContext);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldRejectMapWithMoreThanMaxAllowedElement() {
        final Map<String, String> info = Map.of("key1", "value1", "key2", "value2", "key3", "value3", "key4", "value4");

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        final boolean result = this.analyticsVoInfoSizeValidator.isValid(info, constraintValidatorContext);
        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo("Too many info, 4 found, whereas the maximum allowed is 3");
    }

    @Test
    public void shouldRejectMapWithKeyTooLong() {
        final Map<String, String> info = Map.of(RandomStringUtils.random(infoValidationParameters.getMaxInfoKeyLength() + 1), "value1");

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        final boolean result = this.analyticsVoInfoSizeValidator.isValid(info, constraintValidatorContext);
        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo("Key with more than 10 characters is not allowed, found 11 characters");
    }

    @Test
    public void shouldRejectMapWithValueTooLong() {
        final Map<String, String> info = Map.of("key1", RandomStringUtils.random(infoValidationParameters.getMaxInfoValueLength() + 1));

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        final boolean result = this.analyticsVoInfoSizeValidator.isValid(info, constraintValidatorContext);
        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo("Parameter value with more than 20 characters is not allowed, found 21 characters");
    }
}