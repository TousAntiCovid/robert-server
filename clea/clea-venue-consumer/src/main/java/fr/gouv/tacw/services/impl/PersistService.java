package fr.gouv.tacw.services.impl;

import fr.gouv.tacw.data.DetectedVenue;
import fr.gouv.tacw.data.IDetectedVenueRepository;
import fr.gouv.tacw.services.IPersistService;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.CleaEncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class PersistService implements IPersistService {

    private final IDetectedVenueRepository repository;

    private final int retentionDuration;

    @Autowired
    public PersistService(
            IDetectedVenueRepository repository,
            @Value("${clea.conf.retentionDuration}") int retentionDuration
    ) {
        this.repository = repository;
        this.retentionDuration = retentionDuration;
    }

    @Override
    public void checkAndPersist(DetectedVenue detectedVenue) {
        repository.save(detectedVenue);
        if (this.verify(detectedVenue).isPresent()) {
            repository.save(this.verify(detectedVenue).get());
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "${clea.conf.scheduling.purge.cron}")
    public void deleteDetectedVenues() {
        this.repository.deleteAllByQrCodeScanTimeBefore(Instant.now().truncatedTo(ChronoUnit.MINUTES).minus(retentionDuration, ChronoUnit.DAYS));
    }

    private Optional<DetectedVenue> verify(DetectedVenue detectedVenue) {
        if (!this.isFresh(detectedVenue)) {
            log.warn("");  // FIXME
            return Optional.empty();
        } else if (!this.isComputedTemporaryLocationPublicIdValid(detectedVenue)) {
            log.warn("");  // FIXME
            return Optional.empty();
        }
        log.info("");  // FIXME
        return Optional.of(this.setExposureTime(detectedVenue));
    }

    private boolean isComputedTemporaryLocationPublicIdValid(DetectedVenue detectedVenue) {
        try {
            UUID computed = computed = new CleaEciesEncoder().computeLocationTemporaryPublicId(detectedVenue.getLocationTemporarySecretKey());
            return computed.equals(UUID.fromString(detectedVenue.getLocationTemporaryPublicId()));
        } catch (CleaEncryptionException e) {
            return false;
        }
    }

    private boolean isFresh(DetectedVenue detectedVenue) {
        double qrCodeRenewalInterval = (detectedVenue.getQrCodeRenewalIntervalExponentCompact() == 0x1F)
                ? 0 : Math.pow(2, detectedVenue.getQrCodeRenewalIntervalExponentCompact());
        if (qrCodeRenewalInterval <= 0)
            return true;
        return Math.abs(fr.inria.clea.lsp.utils.TimeUtils.ntpTimestampFromInstant(detectedVenue.getQrCodeScanTime()) - detectedVenue.getQrCodeValidityStartTime()) < (qrCodeRenewalInterval + 300 + 300);
    }

    private DetectedVenue setExposureTime(DetectedVenue detectedVenue) {
        // FIXME implement when specs are precise
        return detectedVenue;
    }
}
