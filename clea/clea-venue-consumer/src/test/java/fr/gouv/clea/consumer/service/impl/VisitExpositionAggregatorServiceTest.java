package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
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
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class VisitExpositionAggregatorServiceTest {

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private IVisitExpositionAggregatorService service;


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
    @DisplayName("visits with no existing context should be saved in DB")
    void saveWithNoContext() {
        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .countryCode(33)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(0)
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
                .countryCode(33)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(0)
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
                .countryCode(33)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(0)
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
    @DisplayName("test how many slots are generated for a given visit")
    void testSlotGeneration() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todayAtMidnightAsNtp = TimeUtils.ntpTimestampFromInstant(todayAtMidnight);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);

        Visit visit = Visit.builder()
                .version(0)
                .type(0)
                .countryCode(33)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(0)
                .venueCategory2(0)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (todayAtMidnightAsNtp / 3600))
                .qrCodeValidityStartTime(0)
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
         *  => slots to generate = 3 before & scanTimeSlot & 3 after = 7
         *  => firstExposedSlot = 16-3 = 13
         *  => lastExposedSlot = 16+3 = 19
         */

        assertThat(repository.count()).isEqualTo(7L);
        List<ExposedVisitEntity> entities = repository.findAll();
        IntStream.rangeClosed(13, 19)
                .forEach(step -> assertThat(entities.stream().filter(it -> it.getTimeSlot() == step).count()).isEqualTo(1));
    }
}