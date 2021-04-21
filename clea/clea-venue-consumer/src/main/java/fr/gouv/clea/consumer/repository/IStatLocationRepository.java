package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IStatLocationRepository extends JpaRepository<StatLocation, StatLocationKey> {
}
