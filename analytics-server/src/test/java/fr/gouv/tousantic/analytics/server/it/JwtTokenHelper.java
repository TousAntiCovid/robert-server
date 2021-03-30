package fr.gouv.tousantic.analytics.server.it;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.util.Base64Utils;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZonedDateTime;
import java.util.Date;

public class JwtTokenHelper {

    private final RSASSASigner rsassaSigner;
    private final JWTClaimsSet.Builder builder;

    public JwtTokenHelper(final String rsaPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {

        final byte[] privateKeyAsByteArray = Base64Utils.decodeFromString(rsaPrivateKey);

        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyAsByteArray);
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final PrivateKey privateKey = kf.generatePrivate(keySpec);

        rsassaSigner = new RSASSASigner(privateKey);
        builder = new JWTClaimsSet.Builder();
    }

    public JwtTokenHelper withExpirationDate(final ZonedDateTime expirationDate) {
        final Date expiration = Date.from(expirationDate.toInstant());
        builder.expirationTime(expiration);
        return this;
    }

    public JwtTokenHelper withIssueTime(final ZonedDateTime issueDate) {
        final Date issue = Date.from(issueDate.toInstant());
        builder.issueTime(issue);
        return this;
    }

    public JwtTokenHelper withJti(final String jti) {
        builder.jwtID(jti);
        return this;
    }

    public String generateToken() throws JOSEException {

        final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).build();
        final JWTClaimsSet body = this.builder.build();

        final SignedJWT signedJWT = new SignedJWT(header, body);
        signedJWT.sign(rsassaSigner);
        return signedJWT.serialize();
    }

    public String generateAuthorizationHeader() throws JOSEException {
        return "Bearer " + generateToken();
    }

}
