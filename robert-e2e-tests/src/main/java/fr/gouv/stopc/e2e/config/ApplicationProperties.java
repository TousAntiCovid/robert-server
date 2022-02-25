package fr.gouv.stopc.e2e.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.net.URL;

@Validated
@ConstructorBinding
@ConfigurationProperties("robert")
@Data
public class ApplicationProperties {

    private final URL wsRestBaseUrl;

    private final String cryptoPublicKey;

    private final String batchCommand;

    private final String batchCommandDown;

    private final String managementUrlWs;

    private final String managementUrlCryptoServer;
}
