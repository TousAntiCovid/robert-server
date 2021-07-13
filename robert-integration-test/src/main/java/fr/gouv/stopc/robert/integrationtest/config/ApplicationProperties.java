package fr.gouv.stopc.robert.integrationtest.config;

import lombok.*;
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
}
