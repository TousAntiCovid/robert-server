package test.fr.gouv.stopc.submission.code.server.database.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.stopc.submission.code.server.database.entity.SequenceFichier;
import fr.gouv.stopc.submission.code.server.database.repository.SequenceFichierRepository;
import fr.gouv.stopc.submission.code.server.database.service.impl.SequenceFichierServiceImpl;

@ExtendWith(SpringExtension.class)
public class SequenceFichierServiceImplTest {

	@InjectMocks
	private SequenceFichierServiceImpl service;

	@Mock
	private SequenceFichierRepository repository;

	@Test
	public void testGetSequenceWhenSequenceNotFound() {

		// Given
		OffsetDateTime now = OffsetDateTime.now();

		when(this.repository.findByAnneeAndMoisAndJour(now.getYear(), now.getMonthValue(), now.getDayOfMonth())).thenReturn(Optional.empty());
		when(this.repository.saveAndFlush(any())).thenReturn(SequenceFichier.builder().build());

		// When
		Optional<SequenceFichier> sequence = this.service.getSequence(now);

		// Then
		assertTrue(sequence.isPresent());
		verify(this.repository).findByAnneeAndMoisAndJour(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
		verify(this.repository).saveAndFlush(any());
	}

	@Test
	public void testGetSequenceWhenSequenceWhenFound() {

		// Given
		OffsetDateTime now = OffsetDateTime.now();
		int year = now.getYear();
		int currentSequence = (year % 100) + 1 ;
		SequenceFichier sequenceFichier = SequenceFichier.builder()
				.id(1L)
				.annee(year)
				.mois(now.getMonthValue())
				.jour(now.getDayOfMonth())
				.sequence(currentSequence)
				.build();


		when(this.repository.findByAnneeAndMoisAndJour(now.getYear(), now.getMonthValue(), now.getDayOfMonth()))
		.thenReturn(Optional.of(sequenceFichier));

		when(this.repository.save(any())).thenReturn(SequenceFichier.builder().build());

		// When
		Optional<SequenceFichier> sequence = this.service.getSequence(now);

		// Then
		assertTrue(sequence.isPresent());
		assertEquals(currentSequence + 1, sequence.get().getSequence());
		verify(this.repository).findByAnneeAndMoisAndJour(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
		verify(this.repository).saveAndFlush(any());
	}
}
