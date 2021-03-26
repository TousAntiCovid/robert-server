package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface IExposedVisitRepository extends JpaRepository<ExposedVisitEntity, String> {

    int deleteAllByQrCodeScanTimeBefore(Instant qrCodeScanTime);

    List<ExposedVisitEntity> findAllByLocationTemporaryPublicIdAndPeriodStart(String locationTemporaryPublicId, long periodStart);
}
