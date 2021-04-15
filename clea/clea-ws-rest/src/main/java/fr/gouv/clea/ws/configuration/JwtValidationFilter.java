package fr.gouv.clea.ws.configuration;

import fr.gouv.clea.ws.exception.CleaForbiddenException;
import fr.gouv.clea.ws.exception.CleaUnauthorizedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@Slf4j
public class JwtValidationFilter extends GenericFilterBean {

    private final boolean checkAuthorization;

    private final String robertJwtPublicKey;

    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtValidationFilter(boolean checkAuthorization, String robertJwtPublicKey, HandlerExceptionResolver handlerExceptionResolver) {
        this.checkAuthorization = checkAuthorization;
        this.robertJwtPublicKey = robertJwtPublicKey;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            if (this.checkAuthorization) {
                String auth = ((HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
                this.checkAuthorization(auth);
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException((HttpServletRequest) request, (HttpServletResponse) response, null, e);
        }
    }

    public void checkAuthorization(String jwtToken) throws CleaForbiddenException {
        if (jwtToken == null) {
            log.warn("Missing Authorisation header!");
            throw new CleaUnauthorizedException();
        }
        jwtToken = jwtToken.replace("Bearer ", "");
        this.verifyJWT(jwtToken);
    }

    private void verifyJWT(String token) throws CleaForbiddenException {
        try {
            byte[] encoded = Decoders.BASE64.decode(this.robertJwtPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance(SignatureAlgorithm.RS256.getFamilyName());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            PublicKey jwtPublicKey = keyFactory.generatePublic(keySpec);
            Jwts.parserBuilder().setSigningKey(jwtPublicKey).build().parseClaimsJws(token);
        } catch (Exception e) {
            log.warn("Failed to verify JWT token!", e);
            throw new CleaForbiddenException();
        }
    }
}
