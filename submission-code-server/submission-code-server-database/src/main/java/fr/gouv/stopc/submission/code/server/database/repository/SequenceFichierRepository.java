package fr.gouv.stopc.submission.code.server.database.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.gouv.stopc.submission.code.server.database.entity.SequenceFichier;

@Repository
public interface SequenceFichierRepository extends JpaRepository<SequenceFichier, Long> {

	Optional<SequenceFichier> findByAnnee(int annee);
	Optional<SequenceFichier> findByAnneeAndMoisAndJour(int annee, int mois, int jour);
}
