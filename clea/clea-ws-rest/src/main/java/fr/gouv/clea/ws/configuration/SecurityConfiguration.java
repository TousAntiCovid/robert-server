package fr.gouv.clea.ws.configuration;

import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class SecurityConfiguration {

    @Bean
    @RequestScope
    public LocationSpecificPartDecoder getLocationSpecificPartDecoder() {
        return new LocationSpecificPartDecoder(null);
    }
}
