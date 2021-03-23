package fr.gouv.tacw.data;

import org.springframework.data.repository.CrudRepository;

import java.time.Instant;

public interface IDetectedVenueRepository extends CrudRepository<DetectedVenue, String> {

    void deleteAllByQrCodeScanTimeBefore(Instant qrCodeScanTime);
}
