package fr.gouv.tacw.ws.service.impl;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.gouv.tacw.ws.exception.TacWarningUnauthorizedException;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.utils.PropertyLoader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthorizationServiceImpl implements AuthorizationService {
	@Autowired
	private PropertyLoader propertyLoader;

	public boolean checkAuthorization(String jwtToken) throws TacWarningUnauthorizedException {
		jwtToken = jwtToken.replace("Bearer ", ""); // TODO validate properly
		log.info("Authorization header: " + jwtToken);

		if (!this.propertyLoader.getJwtReportAuthorizationDisabled()) {
			this.verifyJWT(jwtToken);
		}
		return true;
	}

	private void verifyJWT(String token) throws TacWarningUnauthorizedException {
		SignatureAlgorithm algo = SignatureAlgorithm.RS256; // TODO validate with ANSSI which algo to use, move to
															// proper place
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
}
