package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface IExposedVisitRepository extends JpaRepository<ExposedVisitEntity, String> {

    @Modifying
    @Query(
            value = "DELETE FROM EXPOSED_VISITS e WHERE ((?1 - (e.PERIOD_START + (e.TIMESLOT * ?2))) / ?3) > ?4",
            nativeQuery = true
    )
    int purge(long currentNtp, int durationUnitInSeconds, int secondsInDays, int retentionDurationInDays);

    List<ExposedVisitEntity> findAllByLocationTemporaryPublicIdAndPeriodStart(UUID locationTemporaryPublicId, long periodStart);
}
