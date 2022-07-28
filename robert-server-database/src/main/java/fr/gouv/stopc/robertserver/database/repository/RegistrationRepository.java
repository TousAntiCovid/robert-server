package fr.gouv.stopc.robertserver.database.repository;

import fr.gouv.stopc.robertserver.database.model.Registration;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepository extends MongoRepository<Registration, byte[]> {

    /**
     * Retrieve the number of users at risk
     *
     * @return the count result
     */
    @Query(value = "{ atRisk: {$eq: true} }", count = true)
    Long countNbUsersAtRisk();

    /**
     * Retrieve the number of users than epoch exposition with an epochId <= the
     * epochId in parameter.
     *
     * @param minEpochId
     * @return the count result
     */
    @Query(value = "{exposedEpochs:{$elemMatch:{epochId:{$lte: ?0}}}}", count = true)
    Long countNbUsersWithOldEpochExpositions(int minEpochId);
}
