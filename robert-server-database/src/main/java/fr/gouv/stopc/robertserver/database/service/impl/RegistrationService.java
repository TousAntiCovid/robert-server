package fr.gouv.stopc.robertserver.database.service.impl;

import fr.gouv.stopc.robertserver.database.model.Registration;
import fr.gouv.stopc.robertserver.database.repository.RegistrationRepository;
import fr.gouv.stopc.robertserver.database.service.IRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationService implements IRegistrationService {

    private final RegistrationRepository registrationRepository;

    @Override
    public Optional<Registration> createRegistration(byte[] id) {
        return Optional.ofNullable(
                Registration.builder()
                        .permanentIdentifier(id)
                        .build()
        )
                .map(this.registrationRepository::insert);
    }

    @Override
    public Optional<Registration> findById(byte[] id) {
        return this.registrationRepository.findById(id);
    }

    @Override
    public Optional<Registration> saveRegistration(Registration registration) {
        return Optional.ofNullable(registration)
                .map(this.registrationRepository::save);
    }

    @Override
    public void saveAll(final List<Registration> registrations) {

        if (!CollectionUtils.isEmpty(registrations)) {
            this.registrationRepository.saveAll(registrations);
        }
    }

    @Override
    public void delete(Registration registration) {
        Optional.ofNullable(registration).ifPresent(this.registrationRepository::delete);
    }

    @Override
    public void deleteAll() {
        this.registrationRepository.deleteAll();
    }

    @Override
    public List<Registration> findAll() {
        return this.registrationRepository.findAll();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countNbUsersAtRisk() {
        return this.registrationRepository.countNbUsersAtRisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countNbUsersAtRiskAndNotified() {
        return this.registrationRepository.countNbUsersAtRiskAndNotified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countNbUsersAtRiskAndNotNotified() {
        return this.registrationRepository.countNbUsersAtRiskAndNotNotified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countNbUsersNotified() {
        return this.registrationRepository.countNbUsersNotified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countNbExposedUsersButNotAtRisk() {
        return this.registrationRepository.countNbExposedUsersButNotAtRisk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long countNbNotifiedUsersScoredAgain() {
        return this.registrationRepository.countNbNotifiedUsersScoredAgain();
    }

    @Override
    public Long count() {
        return this.registrationRepository.count();
    }

    @Override
    public Long countNbUsersWithOldEpochExpositions(int minEpochId) {
        return this.registrationRepository.countNbUsersWithOldEpochExpositions(minEpochId);
    }

}
