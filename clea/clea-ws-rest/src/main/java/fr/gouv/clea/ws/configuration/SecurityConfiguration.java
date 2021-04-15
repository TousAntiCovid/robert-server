package fr.gouv.clea.ws.configuration;

import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@EnableWebSecurity
@Slf4j
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final boolean checkAuthorization;

    private final String robertJwtPublicKey;

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Autowired
    public SecurityConfiguration(
            @Value("${clea.conf.security.report.checkAuthorization}") boolean checkAuthorization,
            @Value("${clea.conf.security.report.robertJwtPublicKey}") String robertJwtPublicKey,
            HandlerExceptionResolver handlerExceptionResolver
    ) {
        this.checkAuthorization = checkAuthorization;
        this.robertJwtPublicKey = robertJwtPublicKey;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Bean
    public LocationSpecificPartDecoder getLocationSpecificPartDecoder() {
        return new LocationSpecificPartDecoder(null);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/api/clea/*").permitAll()
                .and()
                .addFilterAfter(new JwtValidationFilter(checkAuthorization, robertJwtPublicKey, handlerExceptionResolver), BasicAuthenticationFilter.class)
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
