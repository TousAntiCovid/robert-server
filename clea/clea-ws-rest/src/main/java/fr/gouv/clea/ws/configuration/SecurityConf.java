package fr.gouv.clea.ws.configuration;

import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class SecurityConf {

    private final String serverAuthoritySecretKey;

    @Autowired
    public SecurityConf(
            @Value("${clea.conf.security.crypto.manualCTAuthoritySecretKey}") String serverAuthoritySecretKey
    ) {
        this.serverAuthoritySecretKey = serverAuthoritySecretKey;
    }

    @Bean
    @RequestScope
    public LocationSpecificPartDecoder getLocationSpecificPartDecoder() {
        return new LocationSpecificPartDecoder(serverAuthoritySecretKey);
    }
}
