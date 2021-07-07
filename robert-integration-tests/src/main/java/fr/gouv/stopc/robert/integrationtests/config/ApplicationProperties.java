package fr.gouv.stopc.robert.integrationtests.config;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

@Value
@Validated
@ConstructorBinding
@ConfigurationProperties("robert")
public class ApplicationProperties {
    @Valid
    RobertWsRestProperties wsRest;
}
