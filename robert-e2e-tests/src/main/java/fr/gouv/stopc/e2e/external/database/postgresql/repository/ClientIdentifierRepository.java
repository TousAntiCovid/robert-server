package fr.gouv.stopc.e2e.external.database.postgresql.repository;

import fr.gouv.stopc.e2e.external.database.postgresql.model.ClientIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientIdentifierRepository extends JpaRepository<ClientIdentifier, Long> {

    Optional<ClientIdentifier> findTopByOrderByIdDesc();
}
