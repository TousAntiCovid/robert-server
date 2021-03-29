package fr.gouv.tousantic.robert.analytics.server.config.validation.validator;

import fr.gouv.tousantic.robert.analytics.server.config.validation.ValidationParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class AnalyticsVoInfoSizeValidator implements ConstraintValidator<AnalyticsVoInfoSize, Map<String, String>> {

    private static final String TOO_MANY_INFO_ERROR_MESSAGE = "Too many info, %d found, whereas the maximum allowed is %d";
    private static final String KEY_TOO_LONG_ERROR_MESSAGE = "Key with more than %d characters is not allowed, found %d characters";
    private static final String VALUE_TOO_LONG_ERROR_MESSAGE = "Parameter value with more than %d characters is not allowed, found %d characters";

    private final ValidationParameters validationParameters;

    @Override
    public boolean isValid(final Map<String, String> value, final ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            return true;
        }
        return isValidMapSize(value, context) && areValidKeySizes(value, context) && areValidValueSizes(value, context);
    }


    private boolean isValidMapSize(final Map<String, String> value, final ConstraintValidatorContext context) {
        final int maxInfoAllowed = validationParameters.getInformation().getMaxInfoAllowed();
        if (value.size() > maxInfoAllowed) {
            contextConfigurer(context, TOO_MANY_INFO_ERROR_MESSAGE, value.size(), maxInfoAllowed);
            return false;
        }
        return true;
    }

    private boolean areValidKeySizes(final Map<String, String> value, final ConstraintValidatorContext context) {
        final int maxInfoKeyLength = validationParameters.getInformation().getMaxInfoKeyLength();
        final Optional<String> firstRejectedKey = value.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(s -> s.length() > maxInfoKeyLength)
                .findFirst();

        if (firstRejectedKey.isPresent()) {
            contextConfigurer(context, KEY_TOO_LONG_ERROR_MESSAGE, maxInfoKeyLength, firstRejectedKey.get().length());
            return false;
        }

        return true;
    }

    private boolean areValidValueSizes(final Map<String, String> value, final ConstraintValidatorContext context) {
        final int maxInfoValueLength = validationParameters.getInformation().getMaxInfoValueLength();
        final Optional<String> firstRejectedValue = value.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(s -> s.length() > maxInfoValueLength)
                .findFirst();

        if (firstRejectedValue.isPresent()) {
            contextConfigurer(context, VALUE_TOO_LONG_ERROR_MESSAGE, maxInfoValueLength, firstRejectedValue.get().length());
            return false;
        }

        return true;
    }

    private void contextConfigurer(final ConstraintValidatorContext context, final String messageTemplate, final Object... messageParameters) {
        final String errorMessage = String.format(messageTemplate, messageParameters);
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
    }
}
