package fr.gouv.stopc.e2e.mobileapplication.repository;

import fr.gouv.stopc.e2e.mobileapplication.repository.model.ClientIdentifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientIdentifierRepository extends CrudRepository<ClientIdentifier, Long> {

    Optional<ClientIdentifier> findTopByOrderByIdDesc();
}
