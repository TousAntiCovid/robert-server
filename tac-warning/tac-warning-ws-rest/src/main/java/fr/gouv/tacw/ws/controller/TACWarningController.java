package fr.gouv.tacw.ws.controller;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.dto.ReportResponseDto;
import fr.gouv.tacw.ws.exception.TacWarningUnauthorizedException;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.PropertyLoader;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = { "${controller.path.prefix}" + UriConstants.API_V1 })
public class TACWarningController {

	@Autowired
	private PropertyLoader propertyLoader;

	@Autowired
	private WarningService warningService;
	
	@PostMapping(value = UriConstants.STATUS)
	protected ExposureStatusResponseDto getStatus(@Valid @RequestBody(required = true) ExposureStatusRequestVo statusRequestVo) {
		log.debug("getStatus called");
		boolean atRisk = warningService.getStatus(statusRequestVo);
		return new ExposureStatusResponseDto(atRisk);
	}

	private void verifyJWT(String token) throws TacWarningUnauthorizedException {
		SignatureAlgorithm algo = SignatureAlgorithm.RS256; // TODO validate with ANSSI which algo to use, move to proper place
		PublicKey jwtPublicKey;
		// TODO use Vault to store this key
		try {
			byte[] encoded = Decoders.BASE64.decode(this.propertyLoader.getJwtPublicKey());
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
			jwtPublicKey = keyFactory.generatePublic(keySpec);
		} catch (Exception e) {
			throw new TacWarningUnauthorizedException();
		}

		try {
		  Jwts.parserBuilder().setSigningKey(jwtPublicKey).build().parseClaimsJws(token);
		} catch (Exception e) {
		  throw new TacWarningUnauthorizedException();
		}

	}

	@PostMapping(value = UriConstants.REPORT)
	protected ReportResponseDto reportVisits(@Valid @RequestBody(required = true) ReportRequestVo reportRequestVo,
			@RequestHeader("Authorization") String jwtToken) {
		jwtToken = jwtToken.replace("Bearer ", ""); // TODO validate properly
		log.info("Authorization header: " + jwtToken);
		if (!this.propertyLoader.getJwtReportAuthorizationDisabled()) {
		  verifyJWT(jwtToken);
		}

		log.debug("reportVisits called");
		warningService.reportVisitsWhenInfected(reportRequestVo);
		return new ReportResponseDto(true, "Report successful");
	}
}
