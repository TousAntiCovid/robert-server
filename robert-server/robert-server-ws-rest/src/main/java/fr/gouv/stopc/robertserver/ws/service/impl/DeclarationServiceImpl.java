package fr.gouv.stopc.robertserver.ws.service.impl;

import fr.gouv.stopc.robertserver.ws.config.WsServerConfiguration;
import fr.gouv.stopc.robertserver.ws.dto.declaration.GenerateDeclarationTokenRequest;
import fr.gouv.stopc.robertserver.ws.service.DeclarationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class DeclarationServiceImpl implements DeclarationService {

    //TODO: Test this class

    public static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.RS256;

    private final WsServerConfiguration configuration;

    public DeclarationServiceImpl(final WsServerConfiguration configuration) {
        this.configuration = configuration;
    }

    public Optional<String> generateDeclarationToken(GenerateDeclarationTokenRequest request) {

        log.debug("generateDeclareToken with {}", request.toString());

        try {
            Date issuedAt = new Date();
            String jti = DigestUtils.sha256Hex(String.join("",
                    request.getTechnicalApplicationIdentifier(),
                    String.valueOf(request.getRiskLevel().getValue()),
                    String.valueOf(request.getLatestRiskEpoch())));
            String kid = configuration.getDeclareTokenKid();

            return Optional.of(
                    Jwts.builder()
                    .setId(jti)
                    .setIssuedAt(issuedAt)
                    .setIssuer("tac")
                    .setHeaderParam("type", "JWT")
                    .setHeaderParam("kid", kid)
                    .claim("notificationDateTimestamp", request.getLastStatusRequestTimestamp())
                    .claim("lastContactDateTimestamp", request.getLastContactDateTimestamp())
                    .signWith(getDeclarePrivateKey(), SIGNATURE_ALGORITHM)
                    .compact());

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Creation of declaration JWT token failed ", e);
            return Optional.empty();
        }
    }

    private PrivateKey getDeclarePrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {

        if (configuration.getJwtUseTransientKey()) {
            // In test mode, we generate a transient key
            KeyPair keyPair = Keys.keyPairFor(SIGNATURE_ALGORITHM);
            return keyPair.getPrivate();
        } else {
            byte[] encoded = Decoders.BASE64.decode(configuration.getDeclareTokenPrivateKey());
            KeyFactory keyFactory = KeyFactory.getInstance(SIGNATURE_ALGORITHM.getFamilyName());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return keyFactory.generatePrivate(keySpec);
        }

    }

}