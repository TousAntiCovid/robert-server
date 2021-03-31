package fr.gouv.tac.analytics.server.config.validation.validator;

import static fr.gouv.tac.analytics.server.config.validation.validator.AnalyticsVoInfoSizeValidator.*;

import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tac.analytics.server.config.validation.ValidationParameters;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@ExtendWith(SpringExtension.class)
public class AnalyticsVoInfoSizeValidatorTest {

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

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo(String.format(TOO_MANY_INFO_ERROR_MESSAGE, 4, 3));
    }

    @Test
    public void shouldRejectMapWithKeyTooLong() {
        final Map<String, String> info = Map.of(RandomStringUtils.random(infoValidationParameters.getMaxInfoKeyLength() + 1), "value1");

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        final boolean result = this.analyticsVoInfoSizeValidator.isValid(info, constraintValidatorContext);
        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo(String.format(KEY_TOO_LONG_ERROR_MESSAGE, 10, 11));
    }

    @Test
    public void shouldRejectMapWithValueTooLong() {
        final Map<String, String> info = Map.of("key1", RandomStringUtils.random(infoValidationParameters.getMaxInfoValueLength() + 1));

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        final boolean result = this.analyticsVoInfoSizeValidator.isValid(info, constraintValidatorContext);
        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo(String.format(VALUE_TOO_LONG_ERROR_MESSAGE, 20, 21));
    }
}