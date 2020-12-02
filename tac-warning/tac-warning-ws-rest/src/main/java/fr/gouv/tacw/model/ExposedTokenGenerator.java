package fr.gouv.tacw.model;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.ws.properties.ScoringProperties;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;

public class ExposedTokenGenerator {

	private ScoringProperties scoringProperties;
	/* Salt is used to randomize the UUID */
	public final static int MAX_SALT = 1000;
	private final QRCodeVo qrCode;
	private final long timestamp;

	/**
	 * Used by tests
	 */
	public static int numberOfGeneratedTokens() {
		return MAX_SALT;
	}

	public ExposedTokenGenerator(VisitVo visit, ScoringProperties scoringProperties) {
		this.qrCode = visit.getQrCode();
		this.timestamp = Long.parseLong(visit.getTimestamp());
		this.scoringProperties = scoringProperties;
	}

	/**
	 * Generate the list of all tokens combination
	 */
	public Stream<ExposedStaticVisitEntity> generateAllExposedTokens() {
		return IntStream
				.rangeClosed(1, MAX_SALT)
				.mapToObj(salt -> this.exposedStaticVisitEntityForSalt(salt));
	}
	
	protected ExposedStaticVisitEntity exposedStaticVisitEntityForSalt(int salt) {
		return new ExposedStaticVisitEntity(
				this.hash(salt),
				this.startOfInterval(timestamp),
				this.endOfInterval(timestamp), 
				this.scoringProperties.getStartOfInterval(),
				this.scoringProperties.getEndOfInterval(),
				this.getRiskIncrementFromVenueType(qrCode.getVenueType()));		
	}

	public String hash(int salt) {
		String data = new StringBuilder()
				.append(salt)
				.append(qrCode.getUuid())
				.toString();
		return DigestUtils.sha256Hex(data);
	}

	private long getRiskIncrementFromVenueType(VenueTypeVo venueTypeVo) {
		return scoringProperties.getExposureCountIncrements().getOrDefault(venueTypeVo.toString(), 5);
	}

	private long startOfInterval(long timestamp) {
		return timestamp - this.scoringProperties.getStartOfInterval();
	}

	private long endOfInterval(long timestamp) {
		return timestamp + this.scoringProperties.getEndOfInterval();
	}
}
