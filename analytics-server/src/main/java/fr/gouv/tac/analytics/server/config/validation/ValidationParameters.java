package fr.gouv.tac.analytics.server.config.validation;

import fr.gouv.tac.analytics.server.config.validation.validator.TimestampedEventCollectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Validated
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Component
@ConfigurationProperties(prefix = "analyticsserver.validation.analytics")
public class ValidationParameters {

    @NotNull
    @Valid
    private InfoValidationParameters information;

    @NotNull
    @Valid
    private TimestampedEventValidationParameters event;

    @NotNull
    @Valid
    private TimestampedEventValidationParameters error;

    public TimestampedEventValidationParameters getParameters(final TimestampedEventCollectionType timestampedEventCollectionType) {

        switch (timestampedEventCollectionType) {
            case EVENT:
                return event;
            case ERROR:
                return error;
            default:
                throw new UnsupportedOperationException("This type is not supported : " + timestampedEventCollectionType.toString());
        }
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class InfoValidationParameters {
        private int maxInfoAllowed;
        private int maxInfoKeyLength;
        private int maxInfoValueLength;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class TimestampedEventValidationParameters {
        private int maxElementAllowed;
        private int maxNameLength;
        private int maxDescriptionLength;
    }

}
