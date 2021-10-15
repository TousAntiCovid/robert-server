package fr.gouv.stopc.robert.integrationtest.config;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

@Validated
@ConstructorBinding
@ConfigurationProperties("robert")
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class ApplicationProperties {

    @Valid
    @Getter
    private final RobertWsRestProperties wsRest;

    @Valid
    @Getter
    private final String cryptoPublicKey;

    @Valid
    @Getter
    private final Batch batch;

    @Value
    public static class Batch {
        String command;
    }
}
