package fr.gouv.tacw.database.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.model.ScoreResult;

@Repository
public interface ExposedStaticVisitRepository extends JpaRepository<ExposedStaticVisitEntity, UUID> {
	Optional<ExposedStaticVisitEntity> findByToken(byte[] token);

	@Query("select exposedVisit from ExposedStaticVisitEntity as exposedVisit "
			+ "where exposedVisit.token = :tokenValue "
			+ "and exposedVisit.visitStartTime = :visitStartTime and exposedVisit.visitEndTime = :visitEndTime ")
	Optional<ExposedStaticVisitEntity> findByTokenAndStartEnd(@Param("tokenValue") byte[] token,
			@Param("visitStartTime") long visitStartTime, @Param("visitEndTime") long visitEndTime);

	long deleteByVisitEndTimeLessThan(long timestamp);

	/**
	 * We use a group by clause to include the venueRiskLevel in the query result. It is mandatory as an opaque visit
	 * does not have this information. It will result in a list of exactly one ScoreResult, i.e. one venue risk level,
	 * as we compute the risk for ONE visit.
	 */
	@Query("SELECT new fr.gouv.tacw.database.model.ScoreResult(venueRiskLevel, COALESCE(SUM(exposureCount), 0), COALESCE(MAX(visitEndTime), -1))"
			+ " FROM ExposedStaticVisitEntity WHERE token = :tokenValue" 
		    + " AND ((visitStartTime > :visitTime - startDelta AND visitStartTime < :visitTime + endDelta) "
			+ " OR (visitStartTime = :visitTime - startDelta AND visitEndTime = :visitTime + endDelta) "
			+ " OR (visitEndTime > :visitTime - startDelta AND visitEndTime < :visitTime + endDelta))"
	        + " GROUP BY venueRiskLevel")
	List<ScoreResult> riskScore(@Param("tokenValue") byte[] token, @Param("visitTime") long visitTime);
}
