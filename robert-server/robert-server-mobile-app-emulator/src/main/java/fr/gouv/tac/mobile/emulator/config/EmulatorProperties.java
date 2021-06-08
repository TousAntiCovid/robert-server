package fr.gouv.tac.mobile.emulator.config;

import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "emulator")
@Validated
@Data
public class EmulatorProperties {

    @NotEmpty
    private String robertWsUrl;

    @NotEmpty
    private String robertCryptoPublicKey;

}
