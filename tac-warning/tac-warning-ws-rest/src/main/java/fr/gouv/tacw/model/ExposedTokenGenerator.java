package fr.gouv.tacw.model;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;

public class ExposedTokenGenerator {

	private TacWarningWsRestConfiguration configuration;
	private final QRCodeVo qrCode;
	private final long timestamp;

	/**
	 * Used by tests
	 */
	public int numberOfGeneratedTokens() {
		return this.configuration.getMaxSalt();
	}

    /**
     * Used by tests
     */
    public ExposedTokenGenerator(TacWarningWsRestConfiguration configuration) {
        this.qrCode = null;
        this.timestamp = -1;
        this.configuration = configuration;
    }
        
    public ExposedTokenGenerator(VisitVo visit, TacWarningWsRestConfiguration configuration) {
        super();
		this.qrCode = visit.getQrCode();
		this.timestamp = TimeUtils.roundedTimestamp(Long.parseLong(visit.getTimestamp()));
		this.configuration = configuration;
	}

	/**
	 * Generate the list of all tokens combination
	 */
	public Stream<ExposedStaticVisitEntity> generateAllExposedTokens() {
		return IntStream
				.rangeClosed(1, this.configuration.getMaxSalt())
				.mapToObj(salt -> this.exposedStaticVisitEntityForSalt(salt));
	}
	
	protected ExposedStaticVisitEntity exposedStaticVisitEntityForSalt(int salt) {
		return new ExposedStaticVisitEntity(
				this.hash(salt),
				this.startOfInterval(timestamp),
				this.endOfInterval(timestamp), 
				this.configuration.getStartOfInterval(),
				this.configuration.getEndOfInterval(),
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
		return configuration.getExposureCountIncrements().getOrDefault(venueTypeVo.toString(), 5);
	}

	private long startOfInterval(long timestamp) {
		return timestamp - this.configuration.getStartOfInterval();
	}

	private long endOfInterval(long timestamp) {
		return timestamp + this.configuration.getEndOfInterval();
	}
}
