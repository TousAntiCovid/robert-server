package fr.gouv.tac.analytics.server.config.validation.validator;

import static fr.gouv.tac.analytics.server.controller.CustomExceptionHandler.PAYLOAD_TOO_LARGE;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.CollectionUtils;

import fr.gouv.tac.analytics.server.config.validation.ValidationParameters;
import fr.gouv.tac.analytics.server.controller.vo.TimestampedEventVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class TimestampedEventCollectionValidator implements ConstraintValidator<TimestampedEventCollection, Collection<TimestampedEventVo>> {


    public static final String TOO_MANY_ELEMENTS_ERROR_MESSAGE = "%s Too many %s, %d found, whereas the maximum allowed is %d";
    public static final String NAME_TOO_LONG_ERROR_MESSAGE = "For %s, name with more than %d characters is not allowed, found %d characters";
    public static final String DESCRIPTION_TOO_LONG_ERROR_MESSAGE = "For %s, description with more than %d characters is not allowed, found %d characters";

    private final ValidationParameters validationParameters;

    private TimestampedEventCollectionType timestampedEventCollectionType;
    private ValidationParameters.TimestampedEventValidationParameters parameters;

    @Override
    public void initialize(final TimestampedEventCollection timestampedEventCollection) {
        timestampedEventCollectionType = timestampedEventCollection.type();
        parameters = validationParameters.getParameters(timestampedEventCollectionType);
    }

    @Override
    public boolean isValid(final Collection<TimestampedEventVo> value, final ConstraintValidatorContext context) {
        if (CollectionUtils.isEmpty(value)) {
            return true;
        }
        return isValidCollectionSize(value, context) && areNameValidSizes(value, context) && areDescriptionValidSizes(value, context);
    }

    private boolean isValidCollectionSize(final Collection<TimestampedEventVo> value, final ConstraintValidatorContext context) {
        if (value.size() > parameters.getMaxElementAllowed()) {
            contextConfigurer(context, TOO_MANY_ELEMENTS_ERROR_MESSAGE, PAYLOAD_TOO_LARGE, timestampedEventCollectionType, value.size(), parameters.getMaxElementAllowed());
            return false;
        }
        return true;
    }

    private boolean areNameValidSizes(final Collection<TimestampedEventVo> value, final ConstraintValidatorContext context) {
        final int maxNameLength = parameters.getMaxNameLength();

        final Optional<String> firstRejectedName = getFirstAttributeWithInvalidLength(value, TimestampedEventVo::getName, maxNameLength);

        if (firstRejectedName.isPresent()) {
            contextConfigurer(context, NAME_TOO_LONG_ERROR_MESSAGE, timestampedEventCollectionType, maxNameLength, firstRejectedName.get().length());
            return false;
        }
        return true;
    }

    private boolean areDescriptionValidSizes(final Collection<TimestampedEventVo> value, final ConstraintValidatorContext context) {
        final int maxDescriptionLength = parameters.getMaxDescriptionLength();
        final Optional<String> firstRejectedDescription = getFirstAttributeWithInvalidLength(value, TimestampedEventVo::getDesc, maxDescriptionLength);

        if (firstRejectedDescription.isPresent()) {
            contextConfigurer(context, DESCRIPTION_TOO_LONG_ERROR_MESSAGE, timestampedEventCollectionType, maxDescriptionLength, firstRejectedDescription.get().length());
            return false;
        }
        return true;
    }

    private Optional<String> getFirstAttributeWithInvalidLength(final Collection<TimestampedEventVo> value, final Function<TimestampedEventVo, String> attributeAccessor, final int maxLengthAllowed) {
        return value.stream()
                .map(attributeAccessor)
                .filter(Objects::nonNull)
                .filter(n -> n.length() > maxLengthAllowed)
                .findFirst();
    }

    private void contextConfigurer(final ConstraintValidatorContext context, final String messageTemplate, final Object... messageParameters) {
        final String errorMessage = String.format(messageTemplate, messageParameters);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }
}
