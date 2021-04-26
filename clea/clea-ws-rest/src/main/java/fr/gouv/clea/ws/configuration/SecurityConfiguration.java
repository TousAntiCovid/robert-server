package fr.gouv.clea.ws.configuration;

import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final CleaWsProperties cleaWsProperties;

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Autowired
    public SecurityConfiguration(
            CleaWsProperties cleaWsProperties,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.cleaWsProperties = cleaWsProperties;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Bean
    public LocationSpecificPartDecoder getLocationSpecificPartDecoder() {
        return new LocationSpecificPartDecoder();
    }

    private PublicKey initPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] encoded = Decoders.BASE64.decode(this.cleaWsProperties.getRobertJwtPublicKey());
        KeyFactory keyFactory = KeyFactory.getInstance(SignatureAlgorithm.RS256.getFamilyName());
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return keyFactory.generatePublic(keySpec);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/api/clea/**").permitAll()
                .and()
                .addFilterAfter(new JwtValidationFilter(this.cleaWsProperties.isAuthorizationCheckActive(), this.initPublicKey(), handlerExceptionResolver), BasicAuthenticationFilter.class)
                .httpBasic().disable()
                .csrf().disable()
                .cors();
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers(
                // Swagger UI v2
                "/v2/api-docs",
                "/swagger-resources",
                "/swagger-resources/**",
                "/configuration/ui",
                "/configuration/security",
                "/swagger-ui.html",
                "/webjars/**",
                // Swagger UI v3
                "/v3/api-docs/**",
                "/swagger-ui/**",
                // other public endpoints
                "/actuator/**"
        );
    }
}
