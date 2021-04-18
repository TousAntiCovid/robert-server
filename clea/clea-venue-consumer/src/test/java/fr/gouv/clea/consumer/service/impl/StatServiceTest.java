package fr.gouv.clea.consumer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IStatLocationRepository;
import fr.inria.clea.lsp.utils.TimeUtils;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StatServiceTest {
    @Mock
    IStatLocationRepository repositoryService;
    
    @Captor
    ArgumentCaptor<StatLocation> statLocationCaptor;
    
    StatService service;

    final VenueConsumerConfiguration config = new VenueConsumerConfiguration();
    
    static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);;
    static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);;
    static final long TODAY_AT_MIDNIGHT_AS_NTP = TimeUtils.ntpTimestampFromInstant(TODAY_AT_MIDNIGHT);
    final UUID uuid = UUID.randomUUID();
    final byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);
    final byte[] encryptedLocationContactMessage = RandomUtils.nextBytes(20);

    @BeforeEach
    void init() {
        config.setDurationUnitInSeconds(Duration.ofMinutes(30).toSeconds());
        service = Mockito.spy(new StatService(repositoryService, config));
    }
    
    @Test
    void should_create_a_new_stat_when_visit_has_no_exising_context() {
        /*
         * if:
         * periodStartTime = today at 00:00:00
         * qrCodeScanTime = today at 08:15:00
         * durationUnit = 1800 seconds
         *
         * then:
         *  => scanTimeSlot = 8*2 = 16
         *  => stat duration = periodStartTime + (slot * durationUnit) = today at 08:00:00
         */

        Visit visit = defaultVisit().toBuilder()
            .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
            .venueType(4)
            .venueCategory1(1)
            .venueCategory2(2)
            .build();

        service.logStats(visit);

        verify(service).newStatLocation(any(StatLocationKey.class), eq(visit));
        verify(service, never()).updateStatLocation(any(StatLocation.class), any(Visit.class));
        verify(repositoryService).save(statLocationCaptor.capture());
        StatLocation statLocation = statLocationCaptor.getValue();
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(1L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    void should_update_an_existing_stat_when_visit_has_existing_context() {
        /*
         * if:
         * periodStartTime = today at 00:00:00
         * qrCodeScanTime = today at 08:15:00
         * durationUnit = 1800 seconds
         *
         * then:
         *  => scanTimeSlot = 8*2 = 16
         *  => stat duration = periodStartTime + (slot * durationUnit) = today at 08:00:00
         */
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
                .build();
        service.logStats(visit1);
        StatLocation stat = StatLocation.builder()
                .statLocationKey(StatLocationKey.builder()
                        .period(TODAY_AT_8AM) // scan time slot
                        .venueType(visit1.getVenueType())
                        .venueCategory1(visit1.getVenueCategory1())
                        .venueCategory2(visit1.getVenueCategory2())
                        .build())
                .backwardVisits(1)
                .forwardVisits(0)
                .build();
        when(repositoryService.findById(Mockito.any())).thenReturn(Optional.of(stat));
        Visit visit2 = visit1.toBuilder().build();
        
        service.logStats(visit2);
        
        verify(service).newStatLocation(any(StatLocationKey.class), eq(visit1));
        verify(service).updateStatLocation(any(StatLocation.class), eq(visit2));
        verify(repositoryService, times(2)).save(statLocationCaptor.capture());
        StatLocation statLocation = statLocationCaptor.getAllValues().get(statLocationCaptor.getAllValues().size() - 1);
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(2L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    void should_get_same_stat_period_when_visits_scantime_are_in_same_slot() {
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES)) // same slot
                .build();
        Visit visit3 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(28, ChronoUnit.MINUTES)) // same slot
                .build();

        assertThat(service.getStatPeriod(visit1))
            .isEqualTo(service.getStatPeriod(visit2))
            .isEqualTo(service.getStatPeriod(visit3));
    }

    @Test
    void should_get_new_period_when_different_visit_slots() {
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(31, ChronoUnit.MINUTES)) // different slot
                .build();
        assertThat(service.getStatPeriod(visit1))
            .isNotEqualTo(service.getStatPeriod(visit2));
    }

    @Test
    void should_get_new_context_when_different_venue_type() {
        Visit visit1 = defaultVisit().toBuilder().venueType(1).build(),
            visit2 = defaultVisit().toBuilder().venueType(2).build();
        
        service.logStats(visit1);
        service.logStats(visit2);
        
        verify(service, times(2)).newStatLocation(any(StatLocationKey.class), any(Visit.class));
        verify(service, never()).updateStatLocation(any(StatLocation.class), any(Visit.class));
    }
    
    @Test
    void should_get_new_context_when_different_venue_category1() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory1(1).build(),
            visit2 = defaultVisit().toBuilder().venueCategory1(2).build();
        
        service.logStats(visit1);
        service.logStats(visit2);
        
        verify(service, times(2)).newStatLocation(any(StatLocationKey.class), any(Visit.class));
        verify(service, never()).updateStatLocation(any(StatLocation.class), any(Visit.class));
    }
    
    @Test
    void should_get_new_context_when_different_venue_category2() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory2(1).build(),
            visit2 = defaultVisit().toBuilder().venueCategory2(2).build();
        
        service.logStats(visit1);
        service.logStats(visit2);
        
        verify(service, times(2)).newStatLocation(any(StatLocationKey.class), any(Visit.class));
        verify(service, never()).updateStatLocation(any(StatLocation.class), any(Visit.class));
    }
    
    private Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (TODAY_AT_MIDNIGHT_AS_NTP / 3600))
                .qrCodeValidityStartTime(Instant.now())
                .qrCodeScanTime(Instant.now())
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .isBackward(true)
                .build();
    }

}