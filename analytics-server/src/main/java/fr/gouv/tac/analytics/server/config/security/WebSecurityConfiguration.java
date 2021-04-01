package fr.gouv.tac.analytics.server.config.security;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.DelegatingOAuth2JwtTokenValidator;
import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.ExpirationTokenPresenceOAuth2TokenValidator;
import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.JtiCanOnlyBeUsedOnceOAuth2TokenValidator;
import fr.gouv.tac.analytics.server.config.security.oauth2tokenvalidator.JtiPresenceOAuth2TokenValidator;

@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String RSA_ALGORITHM = "RSA";

    @Autowired
    private HandlerExceptionResolver handlerExceptionResolver;

    @Value("${analyticsserver.robert_jwt_analyticspublickey}")
    private String robertJwtPublicKeyString;

    private RSAPublicKey robertRSAPublicKey;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final byte[] robertJwtPublicKeyBytes = Base64.getDecoder().decode(robertJwtPublicKeyString.getBytes(StandardCharsets.UTF_8));
        final X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(robertJwtPublicKeyBytes, RSA_ALGORITHM);
        final KeyFactory kf = KeyFactory.getInstance(RSA_ALGORITHM);
        robertRSAPublicKey = (RSAPublicKey) kf.generatePublic(X509publicKey);
    }

    @Override
    protected void configure(final HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .httpBasic().disable()
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeRequests()
                .antMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
                .and().oauth2ResourceServer(oauth2 -> oauth2.jwt().and().authenticationEntryPoint(authenticationEntryPoint()))
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> handlerExceptionResolver.resolveException(request, response, null, authException);
    }

    @Bean
    public JwtDecoder jwtDecoder(final JtiPresenceOAuth2TokenValidator jtiPresenceOAuth2TokenValidator,
                                 final JtiCanOnlyBeUsedOnceOAuth2TokenValidator jtiCanOnlyBeUsedOnceOAuth2TokenValidator,
                                 final ExpirationTokenPresenceOAuth2TokenValidator expirationTokenPresenceOAuth2TokenValidator) {
        final NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(robertRSAPublicKey).build();
        final DelegatingOAuth2JwtTokenValidator delegatingTokenValidator = new DelegatingOAuth2JwtTokenValidator(expirationTokenPresenceOAuth2TokenValidator, new JwtTimestampValidator(), jtiPresenceOAuth2TokenValidator, jtiCanOnlyBeUsedOnceOAuth2TokenValidator);
        jwtDecoder.setJwtValidator(delegatingTokenValidator);
        return jwtDecoder;
    }

}
