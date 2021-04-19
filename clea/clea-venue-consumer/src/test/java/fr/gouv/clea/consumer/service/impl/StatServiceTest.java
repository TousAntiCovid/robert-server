package fr.gouv.clea.consumer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IStatLocationRepository;
import fr.gouv.clea.consumer.service.IStatService;
import fr.inria.clea.lsp.utils.TimeUtils;

@SpringBootTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StatServiceTest {

    private static final UUID _UUID = UUID.randomUUID();
    private static final byte[] LOCATION_TEMPORARY_SECRET_KEY = RandomUtils.nextBytes(20);
    private static final byte[] ENCRYPTED_LOCATION_CONTACT_MESSAGE = RandomUtils.nextBytes(20);
    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);
    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);
    private static final long TODAY_AT_MIDNIGHT_AS_NTP = TimeUtils.ntpTimestampFromInstant(TODAY_AT_MIDNIGHT);

    @Autowired
    private IStatLocationRepository repository;
    @Autowired
    private IStatService service;

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void should_create_a_new_stat_in_DB_when_visit_has_no_existing_context() {
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

        List<StatLocation> stats = repository.findAll();
        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(1L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    void should_update_an_existing_stat_in_DB_when_visit_has_existing_context() {
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
        long before = repository.count();

        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
                .build();
        service.logStats(visit2);
        long after = repository.count();

        assertThat(before).isEqualTo(after);

        List<StatLocation> stats = repository.findAll();
        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(2L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    void should_get_same_stat_period_when_visits_scantimes_are_in_same_stat_slot() {
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES)) // same stat slot
                .build();
        Visit visit3 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(28, ChronoUnit.MINUTES)) // same stat slot
                .build();

        service.logStats(visit1);
        service.logStats(visit2);
        service.logStats(visit3);
        
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void should_get_new_period_when_scantimes_are_in_different_stat_slot() {
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(31, ChronoUnit.MINUTES)) // different stat slot
                .build();
        
        service.logStats(visit1);
        service.logStats(visit2);
        
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void should_get_new_context_when_different_venue_type() {
        Visit visit1 = defaultVisit().toBuilder().venueType(1).build(),
                visit2 = defaultVisit().toBuilder().venueType(2).build();

        service.logStats(visit1);
        service.logStats(visit2);
        
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void should_get_new_context_when_different_venue_category1() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory1(1).build(),
                visit2 = defaultVisit().toBuilder().venueCategory1(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void should_get_new_context_when_different_venue_category2() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory2(1).build(),
                visit2 = defaultVisit().toBuilder().venueCategory2(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(repository.count()).isEqualTo(2);
    }
    
    static Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(_UUID)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (TODAY_AT_MIDNIGHT_AS_NTP / 3600))
                .qrCodeValidityStartTime(Instant.now())
                .qrCodeScanTime(Instant.now())
                .locationTemporarySecretKey(LOCATION_TEMPORARY_SECRET_KEY)
                .encryptedLocationContactMessage(ENCRYPTED_LOCATION_CONTACT_MESSAGE)
                .isBackward(true)
                .build();
    }
}