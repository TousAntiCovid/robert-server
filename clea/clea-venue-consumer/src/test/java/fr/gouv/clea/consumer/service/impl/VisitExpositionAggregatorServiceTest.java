package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IVisitExpositionAggregatorService;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
@DirtiesContext
class VisitExpositionAggregatorServiceTest {

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private IVisitExpositionAggregatorService service;

    private Instant yesterday;
    private UUID uuid;
    private byte[] locationTemporarySecretKey;
    private byte[] encryptedLocationContactMessage;

    @BeforeEach
    void init() {
        yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
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
                .periodDuration(3)
                .compressedPeriodStartTime(1062707)
                .qrCodeValidityStartTime(0)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(yesterday)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit);

        assertThat(repository.count()).isEqualTo(21L);
        List<ExposedVisitEntity> entities = repository.findAll();
        entities.forEach(it -> {
                    assertThat(it.getLocationTemporaryPublicId()).isEqualTo(uuid.toString());
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
                .periodDuration(3)
                .compressedPeriodStartTime(1062707)
                .qrCodeValidityStartTime(0)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(yesterday)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit);
        service.updateExposureCount(visit);

        assertThat(repository.count()).isEqualTo(21L);
        List<ExposedVisitEntity> entities = repository.findAll();
        entities.forEach(it -> {
                    assertThat(it.getLocationTemporaryPublicId()).isEqualTo(uuid.toString());
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
                .periodDuration(3)
                .compressedPeriodStartTime(1062707)
                .qrCodeValidityStartTime(0)
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(yesterday)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit);

        visit.setBackward(false);
        service.updateExposureCount(visit);

        UUID newUUID = UUID.randomUUID();
        visit.setLocationTemporaryPublicId(newUUID);
        visit.setBackward(true);
        service.updateExposureCount(visit);

        assertThat(repository.count()).isEqualTo(42L);
        List<ExposedVisitEntity> entities = repository.findAll();
        entities.stream()
                .filter(it -> it.getLocationTemporaryPublicId().equals(uuid.toString()))
                .forEach(it -> {
                            assertThat(it.getLocationTemporaryPublicId()).isEqualTo(uuid.toString());
                            assertThat(it.getBackwardVisits()).isEqualTo(1);
                            assertThat(it.getForwardVisits()).isEqualTo(1);
                        }
                );

        entities.stream()
                .filter(it -> it.getLocationTemporaryPublicId().equals(newUUID.toString()))
                .forEach(it -> {
                            assertThat(it.getLocationTemporaryPublicId()).isEqualTo(newUUID.toString());
                            assertThat(it.getBackwardVisits()).isEqualTo(1);
                            assertThat(it.getForwardVisits()).isEqualTo(0);
                        }
                );
    }

    @Disabled
    @Test
    @DisplayName("test how many slots are generated for a given visit")
    void testSlotGeneration() {
        fail("Not tested yet");
    }
}