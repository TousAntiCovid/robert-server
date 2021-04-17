package fr.gouv.clea.consumer.service.impl;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class SlotGenerationTest {

    static final Instant TODAY_AT_MIDNIGHT =  Instant.now().truncatedTo(DAYS);
    static final Instant TODAY_AT_8AM =  TODAY_AT_MIDNIGHT.plus(8, HOURS);

    final VenueConsumerConfiguration config = VenueConsumerConfiguration.builder().build();
    
    @Mock
    IExposedVisitRepository repository;

    @Captor
    ArgumentCaptor<List<ExposedVisitEntity>> exposedVisitEntitiesCaptor;

    VisitExpositionAggregatorService service;

    @BeforeEach
    void init() {
        config.setDurationUnitInSeconds(Duration.ofMinutes(30).toSeconds());
        service = new VisitExpositionAggregatorService(repository, config);
    }

    @Test
    void a_period_duration_of_24_hours_generates_5_slots() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .qrCodeValidityStartTime(TODAY_AT_MIDNIGHT)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        
        service.updateExposureCount(visit);

        /*  => scanTimeSlot = 8*2 = 16
         *  => slots to generate = 2 before + scanTimeSlot + 2 after = 5
         *  => firstExposedSlot = 16-2 = 14
         *  => lastExposedSlot = 16+2 = 18
         */
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(14, 15, 16, 17, 18);
    }
    
    @Test
    void a_period_duration_of_1_hour_generates_2_slots() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(1)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_8AM))
                .qrCodeValidityStartTime(TODAY_AT_8AM)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        
        service.updateExposureCount(visit);

        /*  => scanTimeSlot = 0
         *  => slots to generate = scanTimeSlot + 1 after = 2
         *  => firstExposedSlot = 0
         *  => lastExposedSlot = 0+1 = 1
         */
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(0, 1);
    }

    @Test
    void a_visit_at_first_slot_with_an_unlimited_period_duration_generates_3_slots() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(255)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_8AM))
                .qrCodeValidityStartTime(TODAY_AT_8AM)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        
        service.updateExposureCount(visit);

        /*
         *  => scanTimeSlot = 0
         *  => slots to generate = scanTimeSlot + 2 after = 3
         *  => firstExposedSlot = 0
         *  => lastExposedSlot = 0+3-1 = 2
         */
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(0, 1, 2);
    }

    @Test
    void a_qrScanTime_after_period_validity_doesnt_generate_slots() {
        Visit visit = defaultVisit().toBuilder()
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .periodDuration(6)
                .qrCodeValidityStartTime(TODAY_AT_MIDNIGHT)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        
        service.updateExposureCount(visit);

        verify(repository, never()).saveAll(exposedVisitEntitiesCaptor.capture());
    }

    @Test
    void a_visit_at_first_slot_when_qrScanTime_is_after_qr_validity_generates_5_slots() {
        // This case can happen with authorized drift
        config.setDurationUnitInSeconds(Duration.ofHours(1).toSeconds());
        Visit visit = defaultVisit().toBuilder()
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .periodDuration(24)
                .qrCodeValidityStartTime(TODAY_AT_MIDNIGHT)
                .qrCodeRenewalIntervalExponentCompact(14) // 2^14 seconds = 4.55 hours 
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        
        service.updateExposureCount(visit);

        /*
         *  => scanTimeSlot = 8
         *  => slots to generate = scanTimeSlot + 2 after + 2 before
         *  => firstExposedSlot = 10
         *  => lastExposedSlot = 6
         */
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(6, 7, 8, 9, 10);
    }
    
    protected Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(UUID.randomUUID())
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(1)
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(RandomUtils.nextBytes(20))
                .encryptedLocationContactMessage(RandomUtils.nextBytes(20))
                .qrCodeScanTime(TODAY_AT_8AM)
                .isBackward(true)
                .build();
    }
    
    protected int getCompressedPeriodStartTime(Instant instant) {
        return (int) (TimeUtils.ntpTimestampFromInstant(instant) / 3600);
    }

}
