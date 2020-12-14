package fr.gouv.tacw.model;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.digest.DigestUtils;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.service.ScoringService;
import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.VisitVo;

public class ExposedTokenGenerator {
    private final TacWarningWsRestConfiguration configuration;
    private QRCodeVo qrCode;
    private long timestamp;
    private final ScoringService scoringService;
    
    public ExposedTokenGenerator(VisitVo visit, TacWarningWsRestConfiguration configuration, ScoringService scoringService) {
        super();
        this.configuration = configuration;
        this.scoringService = scoringService;
        this.qrCode = visit.getQrCode();
        this.timestamp = TimeUtils.roundedTimestamp(Long.parseLong(visit.getTimestamp()));
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
				DatatypeConverter.parseHexBinary(this.hash(salt)),
                this.startOfInterval(timestamp),
                this.endOfInterval(timestamp), 
                this.configuration.getStartDelta(),
                this.configuration.getEndDelta(),
                scoringService.getScoreIncrement(qrCode.getVenueType()));       
    }

    public String hash(int salt) {
        String data = new StringBuilder()
                .append(salt)
                .append(qrCode.getUuid())
                .toString();
        return DigestUtils.sha256Hex(data);
    }

    private long startOfInterval(long timestamp) {
        return timestamp - this.configuration.getStartDelta();
    }

    private long endOfInterval(long timestamp) {
        return timestamp + this.configuration.getEndDelta();
    }

}
