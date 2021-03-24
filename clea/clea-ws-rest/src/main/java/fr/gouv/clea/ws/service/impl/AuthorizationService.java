package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.exception.CleaUnauthorizedException;
import fr.gouv.clea.ws.service.IAuthorizationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@Service
@Slf4j
public class AuthorizationService implements IAuthorizationService {

    private final boolean checkAuthorization;

    private final String robertJwtPublicKey;

    @Autowired
    public AuthorizationService(
            @Value("${clea.conf.security.report.checkAuthorization}") boolean checkAuthorization,
            @Value("${clea.conf.security.report.robertJwtPublicKey}") String robertJwtPublicKey) {
        this.checkAuthorization = checkAuthorization;
        this.robertJwtPublicKey = robertJwtPublicKey;
    }

    public boolean checkAuthorization(String jwtToken) throws CleaUnauthorizedException {
        if (this.checkAuthorization) {
            if (jwtToken == null) {
                throw new CleaUnauthorizedException("Missing Authorisation header!");
            }
            jwtToken = jwtToken.replace("Bearer ", "");
            this.verifyJWT(jwtToken);
        }
        return true;
    }

    private void verifyJWT(String token) throws CleaUnauthorizedException {
        PublicKey jwtPublicKey;
        try {
            byte[] encoded = Decoders.BASE64.decode(this.robertJwtPublicKey);
            KeyFactory keyFactory = KeyFactory.getInstance(SignatureAlgorithm.RS256.getFamilyName());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            jwtPublicKey = keyFactory.generatePublic(keySpec);
            Jwts.parserBuilder().setSigningKey(jwtPublicKey).build().parseClaimsJws(token);
        } catch (Exception e) {
            log.warn("Failed to verify JWT token!", e);
            throw new CleaUnauthorizedException();
        }
    }
}
