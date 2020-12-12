package fr.gouv.tacw.ws.service.impl;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import org.springframework.stereotype.Service;

import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.exception.TacWarningUnauthorizedException;
import fr.gouv.tacw.ws.service.AuthorizationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;

@Service
public class AuthorizationServiceImpl implements AuthorizationService {
	private TacWarningWsRestConfiguration configuration;
	
	public AuthorizationServiceImpl(TacWarningWsRestConfiguration configuration) {
	    super();
		this.configuration = configuration;
	}

	public boolean checkAuthorization(String jwtToken) throws TacWarningUnauthorizedException {
		jwtToken = jwtToken.replace("Bearer ", ""); // TODO validate properly

		if (!this.configuration.isJwtReportAuthorizationDisabled()) {
			this.verifyJWT(jwtToken);
		}
		return true;
	}

	private void verifyJWT(String token) throws TacWarningUnauthorizedException {
		PublicKey jwtPublicKey;
		try {
			byte[] encoded = Decoders.BASE64.decode(this.configuration.getRobertJwtPublicKey());
			KeyFactory keyFactory = KeyFactory.getInstance(algo.getFamilyName());
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
			jwtPublicKey = keyFactory.generatePublic(keySpec);
			Jwts.parserBuilder().setSigningKey(jwtPublicKey).build().parseClaimsJws(token);
		} catch (Exception e) {
			throw new TacWarningUnauthorizedException();
		}
	}
}
