package fr.gouv.stopc.robertserver.database.service;

import fr.gouv.stopc.robertserver.database.model.Registration;

import java.util.List;
import java.util.Optional;

public interface IRegistrationService {

    Optional<Registration> createRegistration(byte[] id);

    Optional<Registration> findById(byte[] id);

    Optional<Registration> saveRegistration(Registration registration);

    void saveAll(List<Registration> registrations);

    void delete(Registration registration);

    void deleteAll();

    List<Registration> findAll();

    /**
     * Return the number of users detected at risk (atRisk=true)
     */
    Long countNbUsersAtRisk();

    Long count();

    /**
     * Retrieve the number of users with old epoch exposition.
     *
     * @param minEpochId filter the registration with epoch exposition with an epoch
     *                   id <= minEpochId
     * @return the number of users
     */
    Long countNbUsersWithOldEpochExpositions(int minEpochId);

}
