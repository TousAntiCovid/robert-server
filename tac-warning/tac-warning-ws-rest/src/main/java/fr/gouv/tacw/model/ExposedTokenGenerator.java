package fr.gouv.tacw.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.codec.digest.DigestUtils;

import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;

public class ExposedTokenGenerator {
	/* Salt is used to randomize the tuple token, timestamp */
	public final static int MAX_SALT = 1000;
	private String uuid;
	private long timestamp;
	private TokenTypeVo tokenTypeVo;
	private ArrayList<VisitTokenVo> exposedTokens;

	/**
	 * Used by tests
	 */
	public static int numberOfGeneratedTokens() {
		return MAX_SALT * 3; // H-1, H, H+1
	}
	
	public ExposedTokenGenerator(VisitVo visit) {
		this.uuid = visit.getQrCode().getUuid();
		this.timestamp = Long.parseLong(visit.getTimestamp());
		this.tokenTypeVo = visit.getQrCode().getType();
		this.exposedTokens = new ArrayList<VisitTokenVo>();
	}
	
	/**
	 * Generate the list of all tokens combination (salt * timestamp range)
	 */
	public List<VisitTokenVo> generateAllExposedTokens() {
		IntStream.rangeClosed(1, MAX_SALT).forEach(salt -> this.generateAllExposedTokens(salt));
		return exposedTokens;
	}

	/**
	 * Generate tokens for H-1, H, H+1
	 */
	private void generateAllExposedTokens(int salt) {
		this.addExposedToken( this.hash(salt, this.uuid, this.timestamp - 1) );
		this.addExposedToken( this.hash(salt, this.uuid, this.timestamp) );
		this.addExposedToken( this.hash(salt, this.uuid, this.timestamp + 1) );
	}
	
	private void addExposedToken(String hash) {
		VisitTokenVo token = new VisitTokenVo(this.tokenTypeVo, hash);
		exposedTokens.add(token);
	}

	public String hash(int salt, String uuid, long timestamp) {
		String data = new StringBuilder().
				append(salt).
				append(uuid).
				append(timestamp).
				toString();
		return DigestUtils.sha256Hex(data);
	}

}
