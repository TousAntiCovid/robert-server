package fr.gouv.tacw.database.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;

@Repository
public interface ExposedStaticVisitRepository extends JpaRepository<ExposedStaticVisitEntity, Long> {
	Optional<ExposedStaticVisitEntity> findByToken(String token);

	@Query("select exposedVisit from ExposedStaticVisitEntity as exposedVisit "
			+ "where exposedVisit.token = :tokenValue "
			+ "and exposedVisit.visitStartTime = :visitStartTime and exposedVisit.visitEndTime = :visitEndTime ")
	Optional<ExposedStaticVisitEntity> findByTokenAndStartEnd(@Param("tokenValue") String token,
			@Param("visitStartTime") long visitStartTime, @Param("visitEndTime") long visitEndTime);

	long deleteByVisitEndTimeLessThan(long timestamp);

	@Query("SELECT COALESCE(SUM(exposureCount), 0)"
			+ " FROM ExposedStaticVisitEntity WHERE token = :tokenValue" 
		    + " AND ((visitStartTime > :visitTime - startDelta AND visitStartTime < :visitTime + endDelta) "
			+ " OR (visitStartTime = :visitTime - startDelta AND visitEndTime = :visitTime + endDelta) "
			+ " OR (visitEndTime > :visitTime - startDelta AND visitEndTime < :visitTime + endDelta))")
	long riskScore(@Param("tokenValue") String token, @Param("visitTime") long visitTime);
}
