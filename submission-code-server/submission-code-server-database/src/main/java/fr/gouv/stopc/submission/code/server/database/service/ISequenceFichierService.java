package fr.gouv.stopc.submission.code.server.database.service;

import java.time.OffsetDateTime;
import java.util.Optional;

import fr.gouv.stopc.submission.code.server.database.entity.SequenceFichier;

public interface ISequenceFichierService {
	
	Optional<SequenceFichier> getSequence(OffsetDateTime date);

}
