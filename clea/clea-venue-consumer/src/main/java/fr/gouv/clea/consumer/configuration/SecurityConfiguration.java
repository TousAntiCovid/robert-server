package fr.gouv.clea.consumer.configuration;

import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfiguration {

    private final String serverAuthoritySecretKey;

    @Autowired
    public SecurityConfiguration(
            @Value("${clea.conf.security.crypto.manualCTAuthoritySecretKey}") String serverAuthoritySecretKey) {
        this.serverAuthoritySecretKey = serverAuthoritySecretKey;
    }

    @Bean
    public LocationSpecificPartDecoder getLocationSpecificPartDecoder() {
        return new LocationSpecificPartDecoder(serverAuthoritySecretKey);
    }

    @Bean
    public CleaEciesEncoder getCleaEciesEncoder() {
        return new CleaEciesEncoder();
    }
}
