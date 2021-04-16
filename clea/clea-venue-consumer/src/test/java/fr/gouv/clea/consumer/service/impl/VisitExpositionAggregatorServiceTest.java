package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IStatService;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@DirtiesContext
class VisitExpositionAggregatorServiceTest {

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private IVisitExpositionAggregatorService service;

    @MockBean
    private IStatService statService;

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

        doNothing().when(statService).logStats(any(Visit.class));
    }

    @AfterEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("visits with no existing context should be saved in DB")
    void saveWithNoContext() {
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit);

        List<ExposedVisitEntity> entities = repository.findAll();
        entities.forEach(it -> {
                    assertThat(it.getLocationTemporaryPublicId()).isEqualTo(uuid);
                    assertThat(it.getBackwardVisits()).isEqualTo(1);
                }
        );
    }

    @Test
    @DisplayName("visits with existing context should be updated in DB")
    void updateWithExistingContext() {
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit);
        long before = repository.count();

        service.updateExposureCount(visit);
        long after = repository.count();

        assertThat(before).isEqualTo(after);

        List<ExposedVisitEntity> entities = repository.findAll();
        entities.forEach(it -> {
                    assertThat(it.getLocationTemporaryPublicId()).isEqualTo(uuid);
                    assertThat(it.getBackwardVisits()).isEqualTo(2);
                }
        );
    }

    @Test
    @DisplayName("new visits should be saved while existing be updated in DB")
    void mixedContext() {
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit);

        visit.setBackward(false);
        service.updateExposureCount(visit);

        UUID newUUID = UUID.randomUUID();
        visit.setLocationTemporaryPublicId(newUUID);
        visit.setBackward(true);
        service.updateExposureCount(visit);

        List<ExposedVisitEntity> entities = repository.findAll();
        entities.stream()
                .filter(it -> it.getLocationTemporaryPublicId().equals(uuid))
                .forEach(it -> {
                            assertThat(it.getLocationTemporaryPublicId()).isEqualTo(uuid);
                            assertThat(it.getBackwardVisits()).isEqualTo(1);
                            assertThat(it.getForwardVisits()).isEqualTo(1);
                        }
                );

        entities.stream()
                .filter(it -> it.getLocationTemporaryPublicId().equals(newUUID))
                .forEach(it -> {
                            assertThat(it.getLocationTemporaryPublicId()).isEqualTo(newUUID);
                            assertThat(it.getBackwardVisits()).isEqualTo(1);
                            assertThat(it.getForwardVisits()).isZero();
                        }
                );
    }

    @Test
    @DisplayName("test how many slots are generated for a given visit with a period duration of 24 hour")
    void testSlotGeneration() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAtMidnight))
                .qrCodeValidityStartTime(todayAtMidnight)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();

        service.updateExposureCount(visit);

        /*
         * if:
         * periodDuration = 24 hours
         * periodStartTime = today at 00:00:00
         * qrCodeScanTime = today at 08:00:00
         * durationUnit = 1800 seconds
         * exposureTime = 3
         *
         * then:
         *  => scanTimeSlot = 8*2 = 16
         *  => slots to generate = 3 before + scanTimeSlot + 3 after = 7
         *  => firstExposedSlot = 16-3 = 13
         *  => lastExposedSlot = 16+3 = 19
         */
        assertThat(repository.count()).isEqualTo(7L);
        List<ExposedVisitEntity> entities = repository.findAll();
        IntStream.rangeClosed(13, 19)
                .forEach(slot -> assertThat(entities.stream().filter(it -> it.getTimeSlot() == slot).count()).isEqualTo(1));
    }

    @Test
    @DisplayName("stop processing if qrCodeScanTime is before periodStartTime")
    void testWhenQrScanIsBeforePeriodStart() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAt8am))
                .qrCodeValidityStartTime(todayAt8am)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAtMidnight)
                .isBackward(true)
                .build();

        service.updateExposureCount(visit);

        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("test how many slots are generated for a given visit with a period duration of 1 hour")
    void testSlotGenerationWithPeriodDuration1() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(1)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAt8am))
                .qrCodeValidityStartTime(todayAt8am)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();

        service.updateExposureCount(visit);

        /*
         * if:
         * periodDuration = 1 hours
         * periodStartTime = today at 08:00:00
         * qrCodeScanTime = today at 08:00:00
         * durationUnit = 1800 seconds
         * exposureTime = 3
         *
         * then:
         *  => scanTimeSlot = 0
         *  => slots to generate = scanTimeSlot + 1 after = 2
         *  => firstExposedSlot = 0
         *  => lastExposedSlot = 0+1 = 1
         */

        assertThat(repository.count()).isEqualTo(2L);
        List<ExposedVisitEntity> entities = repository.findAll();
        IntStream.rangeClosed(0, 1)
                .forEach(step -> assertThat(entities.stream().filter(it -> it.getTimeSlot() == step).count()).isEqualTo(1));
    }

    @Test
    @DisplayName("test how many slots are generated for a given visit with a period duration of 255 hour")
    void testSlotGenerationWithPeriodDuration255() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(255)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAt8am))
                .qrCodeValidityStartTime(todayAt8am)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();

        service.updateExposureCount(visit);

        /*
         * if:
         * periodDuration = 255 hours
         * periodStartTime = today at 08:00:00
         * qrCodeScanTime = today at 08:00:00
         * durationUnit = 1800 seconds
         * exposureTime = 3
         *
         * then:
         *  => scanTimeSlot = 0
         *  => slots to generate = scanTimeSlot + 2 after = 3
         *  => firstExposedSlot = 0
         *  => lastExposedSlot = 0+3-1 = 2
         */
        assertThat(repository.count()).isEqualTo(4L);
        List<ExposedVisitEntity> entities = repository.findAll();
        entities.forEach(it -> System.out.println(it.getTimeSlot()));
        IntStream.rangeClosed(0, 2)
                .forEach(step -> assertThat(entities.stream().filter(it -> it.getTimeSlot() == step).count()).isEqualTo(1));
    }

    @Test
    @DisplayName("no slot should be generated when qrScanTime is after period validity")
    void testSlotGenerationWithQrScanTimeFarFromPeriodDuration() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(1)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAtMidnight))
                .qrCodeValidityStartTime(todayAtMidnight)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();

        service.updateExposureCount(visit);

        /*
         * if:
         * periodDuration = 1 hours
         * periodStartTime = today at 00:00:00
         * qrCodeScanTime = today at 08:00:00
         * durationUnit = 1800 seconds
         * exposureTime = 3
         *
         * then:
         *  => scanTimeSlot = 0
         *  => slots to generate = 0
         */
        assertThat(repository.count()).isZero();
    }

    protected int getCompressedPeriodStartTime(Instant instant) {
        return (int) (TimeUtils.ntpTimestampFromInstant(instant) / 3600);
    }
}