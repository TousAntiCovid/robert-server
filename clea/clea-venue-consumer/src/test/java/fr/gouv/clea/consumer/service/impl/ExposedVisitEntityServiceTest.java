package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.repository.IExposedVisitRepository;
import fr.gouv.clea.consumer.service.IExposedVisitEntityService;
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
class ExposedVisitEntityServiceTest {

    @Autowired
    private IExposedVisitRepository repository;

    @Autowired
    private IExposedVisitEntityService exposedVisitEntityService;

    private static ExposedVisitEntity createExposedVisit() {
        return new ExposedVisitEntity(
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
    void testCanPersistAnExposedVisit() {
        ExposedVisitEntity exposedVisitEntity = createExposedVisit();
        
        ExposedVisitEntity saved = exposedVisitEntityService.persist(exposedVisitEntity);
        
        assertThat(repository.count()).isEqualTo(1);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isInstanceOf(String.class);
        assertThat(saved.getLocationTemporaryPublicId()).isEqualTo(exposedVisitEntity.getLocationTemporaryPublicId());
        assertThat(saved.getVenueType()).isEqualTo(exposedVisitEntity.getVenueType());
        assertThat(saved.getVenueCategory1()).isEqualTo(exposedVisitEntity.getVenueCategory1());
        assertThat(saved.getVenueCategory2()).isEqualTo(exposedVisitEntity.getVenueCategory2());
        assertThat(saved.getPeriodStart()).isEqualTo(exposedVisitEntity.getPeriodStart());
        assertThat(saved.getTimeSlot()).isEqualTo(exposedVisitEntity.getTimeSlot());
        assertThat(saved.getBackwardVisits()).isEqualTo(exposedVisitEntity.getBackwardVisits());
        assertThat(saved.getForwardVisits()).isEqualTo(exposedVisitEntity.getForwardVisits());
        assertThat(saved.getCreatedAt()).isEqualTo(exposedVisitEntity.getCreatedAt());
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isInstanceOf(Instant.class);
    }

}