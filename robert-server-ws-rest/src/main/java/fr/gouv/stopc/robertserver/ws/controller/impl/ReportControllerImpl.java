package fr.gouv.stopc.robertserver.ws.controller.impl;

import fr.gouv.stopc.robertserver.ws.config.WsServerConfiguration;
import fr.gouv.stopc.robertserver.ws.controller.IReportController;
import fr.gouv.stopc.robertserver.ws.dto.ReportBatchResponseDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.service.ContactDtoService;
import fr.gouv.stopc.robertserver.ws.utils.MessageConstants;
import fr.gouv.stopc.robertserver.ws.utils.MetricsService;
import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportControllerImpl implements IReportController {

    public static final SignatureAlgorithm signatureAlgo = SignatureAlgorithm.RS256;

    private final WsServerConfiguration wsServerConfiguration;

    private final ReportControllerDelegate delegate;

    private final ContactDtoService contactDtoService;

    private final MetricsService metricsService;

    private PrivateKey jwtPrivateKey;

    @Override
    public ResponseEntity<ReportBatchResponseDto> reportContactHistory(ReportBatchRequestVo reportBatchRequestVo)
            throws RobertServerException {

        if (!delegate.isReportRequestValid(reportBatchRequestVo)) {
            return ResponseEntity.badRequest().build();
        }

        metricsService.countHelloMessages(reportBatchRequestVo);

        contactDtoService.saveContacts(reportBatchRequestVo.getContacts());

        String token = generateJWT();

        ReportBatchResponseDto reportBatchResponseDto = ReportBatchResponseDto.builder()
                .message(MessageConstants.SUCCESSFUL_OPERATION.getValue())
                .reportValidationToken(token)
                .success(Boolean.TRUE)
                .build();

        return ResponseEntity.ok(reportBatchResponseDto);
    }

    private String generateJWT() {
        try {
            Date now = new Date();
            Date expiration = new Date(now.getTime() + this.wsServerConfiguration.getJwtLifeTime() * 60000);

            return Jwts.builder()
                    .setHeaderParam("type", "JWT")
                    .setIssuedAt(now)
                    .setExpiration(expiration)
                    .signWith(this.getJwtPrivateKey(), signatureAlgo)
                    .compact();

        } catch (Exception e) {
            log.error("JWT token generation failed!", e);
            // Avoid to send an HTTP Error code to the client if only the report validation
            // token generation fails
            return "";
        }
    }

    protected PrivateKey getJwtPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (this.jwtPrivateKey == null) {
            byte[] encoded = Decoders.BASE64.decode(this.wsServerConfiguration.getJwtPrivateKey());
            KeyFactory keyFactory = KeyFactory.getInstance(signatureAlgo.getFamilyName());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            this.jwtPrivateKey = keyFactory.generatePrivate(keySpec);
        }
        return this.jwtPrivateKey;

    }
}
