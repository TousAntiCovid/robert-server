package fr.gouv.tacw.database.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.gouv.tacw.database.model.StaticVisitTokenEntity;

@Repository
public interface StaticTokenRepository extends JpaRepository<StaticVisitTokenEntity, Long>{
	Optional<String> findByToken(String token);
}
