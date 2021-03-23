package fr.gouv.clea.consumer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;

import java.time.Instant;

public interface IExposedVisitRepository extends JpaRepository<ExposedVisitEntity, String> {

    void deleteAllByQrCodeScanTimeBefore(Instant qrCodeScanTime);
}
