package fr.gouv.tac.analytics.server.controller.mapper;

import fr.gouv.tac.analytics.server.controller.vo.TimestampedEventVo;
import fr.gouv.tac.analytics.server.model.kafka.TimestampedEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

public class TimestampedEventMapperTest {

    private final TimestampedEventMapper timestampedEventMapper = new TimestampedEventMapperImpl();

    @Test
    public void shouldMapWithDescription() {
        final TimestampedEventVo timestampedEventVo = TimestampedEventVo.builder()
                .name("some fancy name")
                .timestamp(ZonedDateTime.now())
                .description("some description")
                .build();

        final TimestampedEvent result = timestampedEventMapper.map(timestampedEventVo);

        Assertions.assertThat(result.getName()).isEqualTo(timestampedEventVo.getName());
        Assertions.assertThat(result.getTimestamp()).isEqualTo(timestampedEventVo.getTimestamp());
        Assertions.assertThat(result.getDescription()).isEqualTo(timestampedEventVo.getDescription());
    }

    @Test
    public void shouldMapWithoutDescription() {
        final TimestampedEventVo timestampedEventVo = TimestampedEventVo.builder()
                .name("some fancy name")
                .timestamp(ZonedDateTime.now())
                .build();

        final TimestampedEvent result = timestampedEventMapper.map(timestampedEventVo);

        Assertions.assertThat(result.getName()).isEqualTo(timestampedEventVo.getName());
        Assertions.assertThat(result.getTimestamp()).isEqualTo(timestampedEventVo.getTimestamp());
        Assertions.assertThat(result.getDescription()).isNull();
    }
}