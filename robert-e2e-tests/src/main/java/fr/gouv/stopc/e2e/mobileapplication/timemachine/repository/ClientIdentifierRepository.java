package fr.gouv.stopc.e2e.mobileapplication.timemachine.repository;

import fr.gouv.stopc.e2e.mobileapplication.timemachine.model.ClientIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientIdentifierRepository extends JpaRepository<ClientIdentifier, Long> {

    Optional<ClientIdentifier> findTopByOrderByIdDesc();

    @Query(value = "select now()", nativeQuery = true)
    String getDate();
}
