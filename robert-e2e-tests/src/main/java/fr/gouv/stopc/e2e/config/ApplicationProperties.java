package fr.gouv.stopc.e2e.config;

import lombok.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import java.net.URL;

@Validated
@ConstructorBinding
@ConfigurationProperties("robert")
@Value
public class ApplicationProperties {

    URL wsRestBaseUrl;

    URL wsRestInternalBaseUrl;

    String cryptoPublicKey;

    DataSourceProperties cryptoDatasource;

    DataSourceProperties captchaDatasource;

    String submissionJwtSigningKey;

    String batchCommand;

    String batchCommandDown;

    Boolean batchLaunchWithKubectl;

    String clientDeviceTypes;

}
