package fr.gouv.stopc.e2e.external.database.mongodb.repository;

import fr.gouv.stopc.e2e.external.database.mongodb.model.Registration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepository extends MongoRepository<Registration, byte[]> {

}
