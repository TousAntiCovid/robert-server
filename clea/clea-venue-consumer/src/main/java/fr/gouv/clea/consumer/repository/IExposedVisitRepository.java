package fr.gouv.clea.consumer.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;

public interface IExposedVisitRepository extends JpaRepository<ExposedVisitEntity, String> {

    int deleteAllByQrCodeScanTimeBefore(Instant qrCodeScanTime);

    List<ExposedVisitEntity> findAllByLocationTemporaryPublicIdAndPeriodStart(UUID locationTemporaryPublicId, long periodStart);
}
