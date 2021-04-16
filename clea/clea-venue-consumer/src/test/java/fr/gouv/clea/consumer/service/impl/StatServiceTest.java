package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IStatLocationRepository;
import fr.gouv.clea.consumer.service.IStatService;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class StatServiceTest {

    @Autowired
    private IStatLocationRepository repository;

    @Autowired
    private IStatService service;

    private Instant todayAtMidnight;
    private Instant todayAt8am;
    private long todayAtMidnightAsNtp;
    private UUID uuid;
    private byte[] locationTemporarySecretKey;
    private byte[] encryptedLocationContactMessage;

    @BeforeEach
    void init() {
        todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        todayAtMidnightAsNtp = TimeUtils.ntpTimestampFromInstant(todayAtMidnight);
        uuid = UUID.randomUUID();
        locationTemporarySecretKey = RandomUtils.nextBytes(20);
        encryptedLocationContactMessage = RandomUtils.nextBytes(20);
    }

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("a visit with no existing context should create a new stat in DB")
    void statWithNoContext() {
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

        Visit visit = createVisit(
                todayAt8am.plus(15, ChronoUnit.MINUTES),
                4,
                1,
                2
        );

        service.logStats(visit);

        List<StatLocation> stats = repository.findAll();
        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(todayAt8am);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(1L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    @DisplayName("a visit with existing context should update an existing stat in DB")
    void statWithExistingContext() {
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

        Visit visit1 = createVisit(
                todayAt8am.plus(15, ChronoUnit.MINUTES),
                4,
                1,
                2
        );
        service.logStats(visit1);
        long before = repository.count();

        Visit visit2 = createVisit(
                todayAt8am.plus(15, ChronoUnit.MINUTES),
                4,
                1,
                2
        );
        service.logStats(visit2);
        long after = repository.count();

        assertThat(before).isEqualTo(after);

        List<StatLocation> stats = repository.findAll();
        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(todayAt8am);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(2L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    @DisplayName("visits in same slot, should be considered as same context")
    void testSameSlot() {
        Visit visit1 = createVisit(
                todayAt8am,
                4,
                1,
                2
        );
        service.logStats(visit1);

        Visit visit2 = createVisit(
                todayAt8am.plus(15, ChronoUnit.MINUTES), // same slot
                4,
                1,
                2
        );
        service.logStats(visit2);

        Visit visit3 = createVisit(
                todayAt8am.plus(28, ChronoUnit.MINUTES), // same slot
                4,
                1,
                2
        );
        service.logStats(visit3);

        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("visits in different slots, should be considered as different contexts")
    void testDifferentSlot() {
        Visit visit1 = createVisit(
                todayAt8am,
                4,
                1,
                2
        );
        service.logStats(visit1);

        Visit visit2 = createVisit(
                todayAt8am.plus(31, ChronoUnit.MINUTES), // different slot
                4,
                1,
                2
        );
        service.logStats(visit2);

        assertThat(repository.count()).isEqualTo(2L);
    }

    @Test
    @DisplayName("context must be determined by [period, venueType, venueCategory1, venueCategory2]")
    void testContext() {
        Visit visit1 = createVisit(
                todayAt8am,
                4,
                1,
                2
        );
        service.logStats(visit1);
        assertThat(repository.count()).isEqualTo(1L);

        // changing just venueType => new context
        Visit visit2 = createVisit(
                todayAt8am,
                3,
                1,
                2
        );
        service.logStats(visit2);
        assertThat(repository.count()).isEqualTo(2L);

        // changing just venueCategory1 => new context
        Visit visit3 = createVisit(
                todayAt8am,
                4,
                2,
                2
        );
        service.logStats(visit3);
        assertThat(repository.count()).isEqualTo(3L);

        // changing just venueCategory2 => new context
        Visit visit4 = createVisit(
                todayAt8am,
                4,
                1,
                3
        );
        service.logStats(visit4);
        assertThat(repository.count()).isEqualTo(4L);

    }

    private Visit createVisit(Instant qrCodeScanTime, int venueType, int venueCategory1, int venueCategory2) {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(venueType)
                .venueCategory1(venueCategory1)
                .venueCategory2(venueCategory2)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(qrCodeScanTime)
                .isBackward(true)
                .build();
    }

}