package fr.gouv.clea.consumer.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface IExposedVisitRepository extends JpaRepository<ExposedVisit, String> {

    void deleteAllByQrCodeScanTimeBefore(Instant qrCodeScanTime);
}
