package fr.gouv.clea.consumer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import fr.inria.clea.lsp.utils.TimeUtils;

@SpringBootTest
@DirtiesContext
class VisitExpositionAggregatorServiceTest {

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private IVisitExpositionAggregatorService service;

    private Instant todayAtMidnight;
    private Instant todayAt8am;
    private UUID uuid;
    private byte[] locationTemporarySecretKey;
    private byte[] encryptedLocationContactMessage;

    @BeforeEach
    void init() {
        todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
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
        Visit visit = defaultVisit().toBuilder()
                .locationTemporaryPublicId(uuid)
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
        Visit visit = defaultVisit().toBuilder()
                .locationTemporaryPublicId(uuid)
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
        Visit visit = defaultVisit().toBuilder()
                .locationTemporaryPublicId(uuid)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit);
        visit.setBackward(false);
        UUID newUUID = UUID.randomUUID();
        Visit visit2 = visit.toBuilder()
                .locationTemporaryPublicId(newUUID)
                .isBackward(true)
                .build();
        
        service.updateExposureCount(visit);
        service.updateExposureCount(visit2);

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
    @DisplayName("stop processing if qrCodeScanTime is before periodStartTime")
    void testWhenQrScanIsBeforePeriodStart() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAt8am))
                .qrCodeValidityStartTime(todayAt8am)
                .qrCodeScanTime(todayAtMidnight)
                .build();
        
        service.updateExposureCount(visit);

        assertThat(repository.count()).isZero();
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