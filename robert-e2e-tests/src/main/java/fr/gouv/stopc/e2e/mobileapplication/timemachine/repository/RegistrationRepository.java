package fr.gouv.stopc.e2e.mobileapplication.timemachine.repository;

import fr.gouv.stopc.e2e.mobileapplication.timemachine.model.Registration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepository extends MongoRepository<Registration, byte[]> {

}
