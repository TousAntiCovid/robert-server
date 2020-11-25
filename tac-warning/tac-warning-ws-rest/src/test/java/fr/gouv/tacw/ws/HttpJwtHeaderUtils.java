package fr.gouv.tacw.ws;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;

public class HttpJwtHeaderUtils {
	private PrivateKey jwtPrivateKey;

	public HttpJwtHeaderUtils(PrivateKey jwtPrivateKey) {
		this.jwtPrivateKey = jwtPrivateKey;
	}

	public HttpJwtHeaderUtils(String jwtPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Decoders.BASE64.decode(jwtPrivateKey));
		KeyFactory keyFactory = KeyFactory.getInstance(AuthorizationService.algo.getFamilyName());
		this.jwtPrivateKey = keyFactory.generatePrivate(privateKeySpec);
	}

	public HttpEntity<ReportRequestVo> getReportEntityWithBearer(ReportRequestVo entity) {
		HttpHeaders headers = new HttpHeaders();
		this.addBearerAuthTo(headers);
		return new HttpEntity<>(entity, headers);
	}

	public HttpHeaders newJsonHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	public HttpHeaders newJsonHeaderWithBearer() {
		HttpHeaders headers = this.newJsonHeader();
		this.addBearerAuthTo(headers);
		return headers;
	}

	public void addBearerAuthTo(HttpHeaders headers) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + 60000);
		try {
			String jwtToken = this.newJwtToken(now, expiration);
			headers.setBearerAuth(jwtToken);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String newJwtToken(Date now, Date expiration) {
		return Jwts.builder()
				.setHeaderParam("type", "JWT")
				.setIssuedAt(now)
				.setExpiration(expiration)
				.signWith(jwtPrivateKey, SignatureAlgorithm.RS256)
				.compact();
	}
}
