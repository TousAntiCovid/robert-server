package fr.gouv.tousantic.analytics.server.config.validation.validator;

import fr.gouv.tousantic.analytics.server.config.validation.ValidationParameters;
import fr.gouv.tousantic.analytics.server.controller.vo.TimestampedEventVo;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.ConstraintValidatorContext;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;

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
        timestampedEventVo.setDescription(null);

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

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo("Too many EVENT, 5 found, whereas the maximum allowed is 3");
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

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo("For EVENT, name with more than 128 characters is not allowed, found 129 characters");
    }

    @Test
    public void shouldRejectCollectionWithDescriptionTooLong() {
        final TimestampedEventVo timestampedEventVo = timestampedEventVoBuilder();
        timestampedEventVo.setDescription(RandomStringUtils.random(eventsValidationParameters.getMaxDescriptionLength() + 1));

        final Collection<TimestampedEventVo> value = Arrays.asList(timestampedEventVo);

        Mockito.when(constraintValidatorContext.buildConstraintViolationWithTemplate(stringArgumentCaptor.capture())).thenReturn(constraintViolationBuilder);

        timestampedEventCollectionSizeValidator.initialize(timestampedEventCollection);
        final boolean result = timestampedEventCollectionSizeValidator.isValid(value, constraintValidatorContext);

        Assertions.assertThat(result).isFalse();

        Assertions.assertThat(stringArgumentCaptor.getValue()).isEqualTo("For EVENT, description with more than 256 characters is not allowed, found 257 characters");
    }


    private TimestampedEventVo timestampedEventVoBuilder() {
        return TimestampedEventVo.builder()
                .name("valid name")
                .timestamp(ZonedDateTime.now())
                .description("some description")
                .build();
    }
}