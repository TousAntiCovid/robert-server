package fr.gouv.stopc.e2e.mobileapplication.repository;

import fr.gouv.stopc.e2e.mobileapplication.repository.model.ClientIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientIdentifierRepository extends JpaRepository<ClientIdentifier, Long> {

    Optional<ClientIdentifier> findTopByOrderByIdDesc();
}
