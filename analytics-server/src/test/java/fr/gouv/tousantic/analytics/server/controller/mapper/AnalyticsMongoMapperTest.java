package fr.gouv.tousantic.analytics.server.controller.mapper;

import fr.gouv.tousantic.analytics.server.model.kafka.Analytics;
import fr.gouv.tousantic.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tousantic.analytics.server.controller.vo.TimestampedEventVo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
public class AnalyticsMongoMapperTest {

    private static final AnalyticsMapper mapper = new AnalyticsMapperImpl();

    @BeforeAll
    public static void setUp() {
        final TimestampedEventMapper timestampedEventMapper = new TimestampedEventMapperImpl();
        ReflectionTestUtils.setField(mapper, "timestampedEventMapper", timestampedEventMapper);
    }

    @Test
    public void shouldFailWhenAnalyticsIsNull() {

        // Given
        final AnalyticsVo analyticsVo = null;

        // When
        final Optional<Analytics> result = mapper.map(analyticsVo);

        // Then
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void shouldSucceedWhenEventsIsNull() {

        // Given
        final AnalyticsVo analyticsVo = getAnalytics();
        analyticsVo.setEvents(null);

        // When
        final Optional<Analytics> result = mapper.map(analyticsVo);

        // Then
        Assertions.assertThat(result).isPresent();

        Assertions.assertThat(result.get().getCreationDate()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        Assertions.assertThat(result.get().getInstallationUuid()).isEqualTo(analyticsVo.getInstallationUuid());
        Assertions.assertThat(result.get().getInfos()).containsExactlyInAnyOrderEntriesOf(analyticsVo.getInfos());
        Assertions.assertThat(result.get().getEvents()).isNull();
        Assertions.assertThat(result.get().getErrors()).hasSameSizeAs(analyticsVo.getErrors());

    }

    @Test
    public void shouldSucceedWhenErrorsIsNull() {

        // Given
        final AnalyticsVo analyticsVo = getAnalytics();
        analyticsVo.setErrors(null);

        // When
        final Optional<Analytics> result = mapper.map(analyticsVo);

        // Then
        Assertions.assertThat(result).isPresent();

        Assertions.assertThat(result.get().getCreationDate()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        Assertions.assertThat(result.get().getInstallationUuid()).isEqualTo(analyticsVo.getInstallationUuid());
        Assertions.assertThat(result.get().getInfos()).containsExactlyInAnyOrderEntriesOf(analyticsVo.getInfos());
        Assertions.assertThat(result.get().getEvents()).hasSameSizeAs(analyticsVo.getEvents());
        Assertions.assertThat(result.get().getErrors()).isNull();

    }


    @Test
    public void shouldSucceedWhenAnalyticsIsValid() {

        // Given
        final AnalyticsVo analyticsVo = getAnalytics();

        // When
        final Optional<Analytics> result = mapper.map(analyticsVo);

        // Then
        Assertions.assertThat(result).isPresent();

        Assertions.assertThat(result.get().getCreationDate()).isEqualToIgnoringSeconds(ZonedDateTime.now());

        Assertions.assertThat(result.get().getInstallationUuid()).isEqualTo(analyticsVo.getInstallationUuid());
        Assertions.assertThat(result.get().getInfos()).containsExactlyInAnyOrderEntriesOf(analyticsVo.getInfos());
        Assertions.assertThat(result.get().getEvents()).hasSameSizeAs(analyticsVo.getEvents());
        Assertions.assertThat(result.get().getErrors()).hasSameSizeAs(analyticsVo.getErrors());

    }

    public AnalyticsVo getAnalytics() {
        final Map<String, String> infos = Map.of("info1", "info1Value", "info2", "info2value");

        final List<TimestampedEventVo> analyticsEvents = new ArrayList<>();
        final List<TimestampedEventVo> analyticsErrors = new ArrayList<>();

        final TimestampedEventVo event = TimestampedEventVo.builder().name("userAcceptedNotificationsInOnboarding").timestamp(ZonedDateTime.now()).description("some description").build();
        final TimestampedEventVo error = TimestampedEventVo.builder().name("ERR432").timestamp(ZonedDateTime.now()).build();
        analyticsEvents.add(event);
        analyticsErrors.add(error);
        return AnalyticsVo.builder()
                .infos(infos)
                .events(analyticsEvents)
                .errors(analyticsErrors)
                .build();

    }
}
