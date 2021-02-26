package fr.gouv.stopc.submission.code.server.database.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import fr.gouv.stopc.submission.code.server.database.entity.SequenceFichier;
import fr.gouv.stopc.submission.code.server.database.repository.SequenceFichierRepository;
import fr.gouv.stopc.submission.code.server.database.service.ISequenceFichierService;

@Service
public class SequenceFichierServiceImpl implements ISequenceFichierService {

	private final SequenceFichierRepository repository;

	@Inject
	public SequenceFichierServiceImpl(SequenceFichierRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<SequenceFichier> getSequence(OffsetDateTime date) {
		
		if(Objects.nonNull(date)) {
			return Optional.ofNullable(this.repository.findByAnneeAndMoisAndJour(date.getYear(), date.getMonthValue(), date.getDayOfMonth())
			.map(sequenceFichier -> {
				sequenceFichier.setSequence(sequenceFichier.getSequence() +  1);
				this.repository.saveAndFlush(sequenceFichier);
				return sequenceFichier;
			})
			.orElseGet(() -> this.createSequence(date)));
		}
		return Optional.empty();
	}

	private SequenceFichier createSequence(OffsetDateTime date) {
		int shortYear = (date.getYear() % 100);
		int sequence = (shortYear  + 1) % 100;
		return this.repository.saveAndFlush(
				SequenceFichier.builder()
				.annee(date.getYear())
				.mois(date.getMonthValue())
				.jour(date.getDayOfMonth())
				.sequence(sequence).build());
	}
}
