package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.data.ExposedVisit;
import fr.gouv.clea.consumer.data.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IPersistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.org.apache.commons.lang.math.RandomUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PersistServiceTest {

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private IPersistService persistService;

    private static ExposedVisit createExposedVisit() {
        return new ExposedVisit(
                null, // handled by db
                UUID.randomUUID().toString(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                RandomUtils.nextInt(),
                RandomUtils.nextLong(),
                RandomUtils.nextLong(),
                Instant.now(),
                null, // handled by db
                null // handled by db
        );
    }

    @BeforeEach
    void init() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("test that persist save to DB")
    void persist() {
        ExposedVisit exposedVisit = createExposedVisit();
        ExposedVisit saved = persistService.persist(exposedVisit);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isInstanceOf(String.class);
        assertThat(saved.getLocationTemporaryPublicId()).isEqualTo(exposedVisit.getLocationTemporaryPublicId());
        assertThat(saved.getVenueType()).isEqualTo(exposedVisit.getVenueType());
        assertThat(saved.getVenueCategory1()).isEqualTo(exposedVisit.getVenueCategory1());
        assertThat(saved.getVenueCategory2()).isEqualTo(exposedVisit.getVenueCategory2());
        assertThat(saved.getPeriodStart()).isEqualTo(exposedVisit.getPeriodStart());
        assertThat(saved.getTimeSlot()).isEqualTo(exposedVisit.getTimeSlot());
        assertThat(saved.getBackwardVisits()).isEqualTo(exposedVisit.getBackwardVisits());
        assertThat(saved.getForwardVisits()).isEqualTo(exposedVisit.getForwardVisits());
        assertThat(saved.getCreatedAt()).isEqualTo(exposedVisit.getCreatedAt());
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isInstanceOf(Instant.class);
    }

}