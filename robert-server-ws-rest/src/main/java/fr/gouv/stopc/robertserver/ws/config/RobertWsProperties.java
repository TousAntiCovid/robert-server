package fr.gouv.stopc.robertserver.ws.config;

import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.net.URL;

@Value
@ConstructorBinding
@ConfigurationProperties(prefix = "robert")
public class RobertWsProperties {

    Captcha captcha;

    Integer epochBundleDurationInDays;

    @Value
    public static class Captcha {

        boolean enabled;

        URL publicBaseUrl;

        URL privateBaseUrl;

        String successCode;
    }
}
