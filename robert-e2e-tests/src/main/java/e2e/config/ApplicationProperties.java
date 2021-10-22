package e2e.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

@Validated
@ConstructorBinding
@ConfigurationProperties("robert")
@Data
public class ApplicationProperties {

    private final String wsRestBaseUrl;

    private final String cryptoPublicKey;
}
