package fr.gouv.clea.consumer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.inria.clea.lsp.utils.TimeUtils;

@ExtendWith(MockitoExtension.class)
public class SlotGenerationTest {
    @Mock
    private VenueConsumerConfiguration config;
    
    @Mock
    private IExposedVisitRepository repository;

    @Captor
    ArgumentCaptor<List<ExposedVisitEntity>> exposedVisitEntitiesCaptor;
    
    @InjectMocks
    @Spy
    private VisitExpositionAggregatorService service;

    private Instant todayAtMidnight;
    private Instant todayAt8am;
    private UUID uuid;
    private byte[] locationTemporarySecretKey;
    private byte[] encryptedLocationContactMessage;

    @BeforeEach
    void init() {
        when(config.getDurationUnitInSeconds()).thenReturn((int) Duration.ofMinutes(30).toSeconds());
        todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        uuid = UUID.randomUUID();
        locationTemporarySecretKey = RandomUtils.nextBytes(20);
        encryptedLocationContactMessage = RandomUtils.nextBytes(20);
    }

    @Test
    @DisplayName("test how many slots are generated for a given visit with a period duration of 24 hour")
    void testSlotGeneration() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAtMidnight))
                .qrCodeValidityStartTime(todayAtMidnight)
                .qrCodeScanTime(todayAt8am)
                .build();
        Mockito.doReturn(3).when(service).getExposureTime(Mockito.anyInt(), anyInt(), anyInt(), anyBoolean());
        
        service.updateExposureCount(visit);

        /*  => scanTimeSlot = 8*2 = 16
         *  => slots to generate = 2 before + scanTimeSlot + 2 after = 5
         *  => firstExposedSlot = 16-2 = 14
         *  => lastExposedSlot = 16+2 = 18
         */
        Mockito.verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue()).hasSize(5);
        List<ExposedVisitEntity> entities = exposedVisitEntitiesCaptor.getValue();
        List<Integer> expectedSlots = IntStream.rangeClosed(14, 18).boxed().collect(Collectors.toList());
        assertThat(entities).extracting(ExposedVisitEntity::getTimeSlot).hasSameElementsAs(expectedSlots);
    }
    
    @Test
    @DisplayName("test how many slots are generated for a visit at first slot with a period duration of 1 hour")
    void testSlotGenerationDoesNotGoOverPeriodValidity() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(1)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAt8am))
                .qrCodeValidityStartTime(todayAt8am)
                .qrCodeScanTime(todayAt8am)
                .build();
        
        service.updateExposureCount(visit);

        /*  => scanTimeSlot = 0
         *  => slots to generate = scanTimeSlot + 1 after = 2
         *  => firstExposedSlot = 0
         *  => lastExposedSlot = 0+1 = 1
         */
        Mockito.verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue()).hasSize(2);
        List<ExposedVisitEntity> entities = exposedVisitEntitiesCaptor.getValue();
        assertThat(entities).extracting(ExposedVisitEntity::getTimeSlot).hasSameElementsAs(List.of(0, 1));
    }

    @Test
    @DisplayName("test how many slots are generated for a visit at first slot with an unlimited period duration")
    void testSlotGenerationWithUnlimitedPeriodDuration() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(255)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAt8am))
                .qrCodeValidityStartTime(todayAt8am)
                .qrCodeScanTime(todayAt8am)
                .build();
        
        service.updateExposureCount(visit);

        /*
         *  => scanTimeSlot = 0
         *  => slots to generate = scanTimeSlot + 2 after = 3
         *  => firstExposedSlot = 0
         *  => lastExposedSlot = 0+3-1 = 2
         */
        Mockito.verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue()).hasSize(3);
        List<ExposedVisitEntity> entities = exposedVisitEntitiesCaptor.getValue();
        assertThat(entities).extracting(ExposedVisitEntity::getTimeSlot).hasSameElementsAs(List.of(0, 1, 2));
    }

    @Test
    @DisplayName("no slot should be generated when qrScanTime is after period validity")
    void testSlotGenerationWithQrScanTimeAfterPeriodValidity() {
        Visit visit = defaultVisit().toBuilder()
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAtMidnight))
                .periodDuration(6)
                .qrCodeValidityStartTime(todayAtMidnight)
                .qrCodeScanTime(todayAt8am)
                .build();
        
        service.updateExposureCount(visit);

        Mockito.verify(repository, never()).saveAll(exposedVisitEntitiesCaptor.capture());
    }

    @Test
    @DisplayName("test how many slots are generated for a visit at first slot when qrScanTime is after qr validity")
    void testSlotGenerationWithQrScanTimeAfterQrValidity() {
        // This case can happen with authorized drift
        when(config.getDurationUnitInSeconds()).thenReturn((int) Duration.ofHours(1).toSeconds());
        Visit visit = defaultVisit().toBuilder()
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAtMidnight))
                .periodDuration(24)
                .qrCodeValidityStartTime(todayAtMidnight)
                .qrCodeRenewalIntervalExponentCompact(14) // 2^14 seconds = 4.55 hours 
                .qrCodeScanTime(todayAt8am)
                .build();
        
        service.updateExposureCount(visit);

        /*
         *  => scanTimeSlot = 8
         *  => slots to generate = scanTimeSlot + 2 after + 2 before
         *  => firstExposedSlot = 10
         *  => lastExposedSlot = 6
         */
        Mockito.verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue()).hasSize(5);
        List<ExposedVisitEntity> entities = exposedVisitEntitiesCaptor.getValue();
        assertThat(entities).extracting(ExposedVisitEntity::getTimeSlot).hasSameElementsAs(List.of(6, 7, 8, 9, 10));
    }
    
    protected Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(1)
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAtMidnight))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();
    }
    
    protected int getCompressedPeriodStartTime(Instant instant) {
        return (int) (TimeUtils.ntpTimestampFromInstant(instant) / 3600);
    }

}
