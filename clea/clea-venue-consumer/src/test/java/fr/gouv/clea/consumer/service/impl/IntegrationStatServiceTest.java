package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IStatLocationRepository;
import fr.gouv.clea.consumer.service.IStatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.gouv.clea.consumer.service.impl.StatServiceTest.defaultVisit;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
class IntegrationStatServiceTest {

    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);
    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);
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

}