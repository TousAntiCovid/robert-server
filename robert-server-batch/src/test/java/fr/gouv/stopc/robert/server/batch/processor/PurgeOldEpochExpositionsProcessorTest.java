package fr.gouv.stopc.robert.server.batch.processor;

import fr.gouv.stopc.robert.server.batch.service.BatchRegistrationService;
import fr.gouv.stopc.robert.server.batch.utils.ProcessorTestUtils;
import fr.gouv.stopc.robertserver.database.model.EpochExposition;
import fr.gouv.stopc.robertserver.database.model.Registration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PurgeOldEpochExpositionsProcessorTest {

    @InjectMocks
    private PurgeOldEpochExpositionsProcessor purgeOldEpochExpositionsProcessor;

    @Mock
    private BatchRegistrationService batchRegistrationService;

    @Test
    public void shouldReturnAnEmptyExposedEpochListInCaseProvidedExposedEpochsIsEmpty() {
        // Given
        Registration registration = Registration.builder().permanentIdentifier(ProcessorTestUtils.generateIdA())
                .build();

        // When
        Registration returnedRegistration = this.purgeOldEpochExpositionsProcessor.process(registration);

        // Then
        assertNotNull(returnedRegistration.getExposedEpochs());
        assertTrue(returnedRegistration.getExposedEpochs().isEmpty());
    }

    @Test
    public void shouldReturnAnEmptyExposedEpochListInCaseProvidedExposedEpochsIsNull() {
        // Given
        Registration registration = Registration.builder().permanentIdentifier(ProcessorTestUtils.generateIdA())
                .build();
        registration.setExposedEpochs(null);

        // When
        Registration returnedRegistration = this.purgeOldEpochExpositionsProcessor.process(registration);

        // Then
        assertNotNull(returnedRegistration.getExposedEpochs());
        assertTrue(returnedRegistration.getExposedEpochs().isEmpty());
    }

    @Test
    public void shouldReturnTheFilteredExposedEpochs() {
        // Given
        Registration registration = Registration.builder().permanentIdentifier(ProcessorTestUtils.generateIdA())
                .build();

        int epochId = 21333;
        Double[] expositionsForFirstEpoch = new Double[] { 1.0 };
        Double[] expositionsForSecondEpoch = new Double[] { 12.5 };
        ArrayList<EpochExposition> expositions = new ArrayList<>();
        EpochExposition notFilteredOutExposedEpoch = EpochExposition.builder()
                .epochId(epochId)
                .expositionScores(Arrays.asList(expositionsForFirstEpoch))
                .build();

        expositions.add(notFilteredOutExposedEpoch);
        expositions.add(
                EpochExposition.builder()
                        .epochId(epochId - (30 * 96))
                        .expositionScores(Arrays.asList(expositionsForSecondEpoch))
                        .build()
        );

        registration.setExposedEpochs(expositions);

        List<EpochExposition> filteredExposedEpochList = Collections.singletonList(notFilteredOutExposedEpoch);

        when(
                batchRegistrationService
                        .getExposedEpochsWithoutEpochsOlderThanContagiousPeriod(any(), anyInt(), anyInt(), anyInt())
        ).thenReturn(filteredExposedEpochList);

        // When
        Registration returnedRegistration = this.purgeOldEpochExpositionsProcessor.process(registration);

        // Then
        assertNotNull(returnedRegistration.getExposedEpochs());
        assertThat(returnedRegistration.getExposedEpochs().size()).isEqualTo(1);
    }
}
