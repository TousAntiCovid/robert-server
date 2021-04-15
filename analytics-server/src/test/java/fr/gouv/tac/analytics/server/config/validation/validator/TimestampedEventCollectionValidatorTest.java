package fr.gouv.tac.analytics.server.config.validation.validator;

import static fr.gouv.tac.analytics.server.config.validation.validator.TimestampedEventCollectionValidator.*;
import static fr.gouv.tac.analytics.server.controller.CustomExceptionHandler.PAYLOAD_TOO_LARGE;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

import javax.validation.ConstraintValidatorContext;

import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tac.analytics.server.config.validation.ValidationParameters;
import fr.gouv.tac.analytics.server.controller.vo.TimestampedEventVo;
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
public class TimestampedEventCollectionValidatorTest {

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Mock
    private TimestampedEventCollection timestampedEventCollection;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidationParameters validationParameters;

    @InjectMocks
    private TimestampedEventCollectionValidator timestampedEventCollectionSizeValidator;

    private ValidationParameters.TimestampedEventValidationParameters eventsValidationParameters;

    @BeforeEach
    public void setUp() {
        eventsValidationParameters = ValidationParameters.TimestampedEventValidationParameters.builder()
                .maxElementAllowed(3)
                .maxDescriptionLength(256)
                .maxNameLength(128)
                .build();

        Mockito.when(timestampedEventCollection.type()).thenReturn(TimestampedEventCollectionType.EVENT);
        Mockito.when(validationParameters.getParameters(TimestampedEventCollectionType.EVENT)).thenReturn(eventsValidationParameters);
    }


    @Test
    public void shouldAcceptNullCollection() {

        final Collection<TimestampedEventVo> value = null;

        timestampedEventCollectionSizeValidator.initialize(timestampedEventCollection);
        final boolean result = timestampedEventCollectionSizeValidator.isValid(value, constraintValidatorContext);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldAcceptCollectionWithLessThanMaxAllowedElement() {

        final Collection<TimestampedEventVo> value = Arrays.asList(timestampedEventVoBuilder());

        timestampedEventCollectionSizeValidator.initialize(timestampedEventCollection);
        final boolean result = timestampedEventCollectionSizeValidator.isValid(value, constraintValidatorContext);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void shouldAcceptCollectionWithNullDescription() {
        final TimestampedEventVo timestampedEventVo = timestampedEventVoBuilder();
        timestampedEventVo.setDesc(null);

        final Collection<TimestampedEventVo> value = Arrays.asList(timestampedEventVo);

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        timestampedEventCollectionSizeValidator.initialize(timestampedEventCollection);
        final boolean result = timestampedEventCollectionSizeValidator.isValid(value, constraintValidatorContext);

        Assertions.assertThat(result).isTrue();

    }

    @Test
    public void shouldRejectCollectionWithTooManyElement() {

        final Collection<TimestampedEventVo> value = Arrays.asList(timestampedEventVoBuilder(), timestampedEventVoBuilder(), timestampedEventVoBuilder(), timestampedEventVoBuilder(), timestampedEventVoBuilder());

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        timestampedEventCollectionSizeValidator.initialize(timestampedEventCollection);
        final boolean result = timestampedEventCollectionSizeValidator.isValid(value, constraintValidatorContext);

        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo(String.format(TOO_MANY_ELEMENTS_ERROR_MESSAGE, PAYLOAD_TOO_LARGE, "EVENT", 5 , 3));
    }

    @Test
    public void shouldRejectCollectionWithNameTooLong() {
        final TimestampedEventVo timestampedEventVo = timestampedEventVoBuilder();
        timestampedEventVo.setName(RandomStringUtils.random(eventsValidationParameters.getMaxNameLength() + 1));

        final Collection<TimestampedEventVo> value = Arrays.asList(timestampedEventVo);

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        timestampedEventCollectionSizeValidator.initialize(timestampedEventCollection);
        final boolean result = timestampedEventCollectionSizeValidator.isValid(value, constraintValidatorContext);

        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo(String.format(NAME_TOO_LONG_ERROR_MESSAGE, "EVENT", 128, 129));
    }

    @Test
    public void shouldRejectCollectionWithDescriptionTooLong() {
        final TimestampedEventVo timestampedEventVo = timestampedEventVoBuilder();
        timestampedEventVo.setDesc(RandomStringUtils.random(eventsValidationParameters.getMaxDescriptionLength() + 1));

        final Collection<TimestampedEventVo> value = Arrays.asList(timestampedEventVo);

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        timestampedEventCollectionSizeValidator.initialize(timestampedEventCollection);
        final boolean result = timestampedEventCollectionSizeValidator.isValid(value, constraintValidatorContext);

        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo(String.format(DESCRIPTION_TOO_LONG_ERROR_MESSAGE, "EVENT", 256, 257));
    }


    private TimestampedEventVo timestampedEventVoBuilder() {
        return TimestampedEventVo.builder()
                .name("valid name")
                .timestamp(ZonedDateTime.now())
                .desc("some description")
                .build();
    }
}