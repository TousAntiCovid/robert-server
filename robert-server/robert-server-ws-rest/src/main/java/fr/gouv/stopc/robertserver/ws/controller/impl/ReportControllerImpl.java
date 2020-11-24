package fr.gouv.stopc.robertserver.ws.controller.impl;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import fr.gouv.stopc.robertserver.ws.controller.IReportController;
import fr.gouv.stopc.robertserver.ws.dto.ReportBatchResponseDto;
import fr.gouv.stopc.robertserver.ws.dto.VerifyResponseDto;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerBadRequestException;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerException;
import fr.gouv.stopc.robertserver.ws.exception.RobertServerUnauthorizedException;
import fr.gouv.stopc.robertserver.ws.service.ContactDtoService;
import fr.gouv.stopc.robertserver.ws.service.IRestApiService;
import fr.gouv.stopc.robertserver.ws.utils.MessageConstants;
import fr.gouv.stopc.robertserver.ws.utils.PropertyLoader;
import fr.gouv.stopc.robertserver.ws.vo.ReportBatchRequestVo;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Objects;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

@Service
@Slf4j
public class ReportControllerImpl implements IReportController {

	private final PropertyLoader propertyLoader;

    private final ContactDtoService contactDtoService;

    private final IRestApiService restApiService;


    @Inject
    public ReportControllerImpl(final ContactDtoService contactDtoService,
		final IRestApiService restApiService,
		final PropertyLoader propertyLoader) {

        this.contactDtoService = contactDtoService;
        this.restApiService = restApiService;
        this.propertyLoader = propertyLoader;
    }

    private boolean areBothFieldsPresent(ReportBatchRequestVo reportBatchRequestVo) {
        return !CollectionUtils.isEmpty(reportBatchRequestVo.getContacts())
                && StringUtils.isNotEmpty(reportBatchRequestVo.getContactsAsBinary());
    }

    private boolean areBothFieldsAbsent(ReportBatchRequestVo reportBatchRequestVo) {
        return Objects.isNull(reportBatchRequestVo.getContacts())
                && StringUtils.isEmpty(reportBatchRequestVo.getContactsAsBinary());
    }

    private String generateJWT(ReportBatchRequestVo reportBatchRequestVo) throws RobertServerException {
      SignatureAlgorithm algo = SignatureAlgorithm.RS256; // TODO validate with ANSSI which algo to use, move to proper place
      PrivateKey jwtPrivateKey;
      try {

	if (this.propertyLoader.getJwtUseTransientKey()) {
			// In test mode, we generate a transient key
			KeyPair keyPair = Keys.keyPairFor(algo);
			jwtPrivateKey = keyPair.getPrivate();
		} else {
			// TODO Use Vault to store the key
			byte[] encoded = Decoders.BASE64.decode(this.propertyLoader.getJwtPrivateKey());
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
			jwtPrivateKey = keyFactory.generatePrivate(keySpec);

		}

		Date now = new Date();
		Date expiration = new Date(now.getTime() + this.propertyLoader.getJwtLifeTime() * 60000);

		String token = Jwts.builder().setHeaderParam("type", "JWT").setIssuedAt(now).setExpiration(expiration)
				.signWith(jwtPrivateKey, algo).compact();

		return token;
      } catch (Exception e) {
		  throw new RobertServerException(); // TODO more precise error
      }
    }

    @Override
    public ResponseEntity<ReportBatchResponseDto> reportContactHistory(ReportBatchRequestVo reportBatchRequestVo) throws RobertServerException {

        if (areBothFieldsPresent(reportBatchRequestVo)) {
            log.warn("Contacts and ContactsAsBinary are both present");
            return ResponseEntity.badRequest().build();
        } else if (Objects.isNull(reportBatchRequestVo.getContacts())) {
            log.warn("Contacts are null. They could be empty([]) but not null");
            return ResponseEntity.badRequest().build();
        }else if (areBothFieldsAbsent(reportBatchRequestVo)) {
            log.warn("Contacts and ContactsAsBinary are both absent");
            return ResponseEntity.badRequest().build();
        }

        if (!this.propertyLoader.getDisableCheckToken())
		    checkValidityToken(reportBatchRequestVo.getToken());

        contactDtoService.saveContacts(reportBatchRequestVo.getContacts());

		String token = generateJWT(reportBatchRequestVo);

		ReportBatchResponseDto reportBatchResponseDto = ReportBatchResponseDto.builder()
				.message(MessageConstants.SUCCESSFUL_OPERATION.getValue()).token(token).success(Boolean.TRUE).build();

		return ResponseEntity.ok(reportBatchResponseDto);

    }

    private void checkValidityToken(String token) throws RobertServerException {

        if (StringUtils.isEmpty(token)) {
            log.warn("No token provided");
            throw new RobertServerBadRequestException(MessageConstants.INVALID_DATA.getValue());
        }

        if (token.length() != 6 && token.length() != 36) {
            log.warn("Token size is incorrect");
            throw new RobertServerBadRequestException(MessageConstants.INVALID_DATA.getValue());
        }

        Optional<VerifyResponseDto> response = this.restApiService.verifyReportToken(token, getCodeType(token));

        if (!response.isPresent() ||  !response.get().isValid()) {
            log.warn("Verifying the token failed");
            throw new RobertServerUnauthorizedException(MessageConstants.INVALID_AUTHENTICATION.getValue());
        }

        log.info("Verifying the token succeeded");
    }

    private String getCodeType(String token) {
        // TODO: create enum for long and short codes
        return token.length() == 6 ? "2" : "1";
    }
}
