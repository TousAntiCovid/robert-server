package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.configuration.VenueConsumerConfiguration;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IStatLocationRepository;
import fr.inria.clea.lsp.utils.TimeUtils;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StatServiceTest {

    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);
    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);
    private static final long TODAY_AT_MIDNIGHT_AS_NTP = TimeUtils.ntpTimestampFromInstant(TODAY_AT_MIDNIGHT);
    private static final UUID _UUID = UUID.randomUUID();
    private static final byte[] LOCATION_TEMPORARY_SECRET_KEY = RandomUtils.nextBytes(20);
    private static final byte[] ENCRYPTED_LOCATION_CONTACT_MESSAGE = RandomUtils.nextBytes(20);
    private final VenueConsumerConfiguration config = new VenueConsumerConfiguration();
    @Mock
    private IStatLocationRepository repositoryService;
    @Captor
    private ArgumentCaptor<StatLocation> statLocationCaptor;
    private StatService service;

    public static Visit defaultVisit() {
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

    @BeforeEach
    void init() {
        config.setDurationUnitInSeconds(Duration.ofMinutes(30).toSeconds());
        config.setStatSlotDurationInSeconds(Duration.ofMinutes(30).toSeconds());
        service = Mockito.spy(new StatService(repositoryService, config));
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

        assertThat(service.getStatPeriod(visit1))
                .isEqualTo(service.getStatPeriod(visit2))
                .isEqualTo(service.getStatPeriod(visit3));
    }

    @Test
    void should_get_new_period_when_scantimes_are_in_different_stat_slot() {
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(31, ChronoUnit.MINUTES)) // different stat slot
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

}