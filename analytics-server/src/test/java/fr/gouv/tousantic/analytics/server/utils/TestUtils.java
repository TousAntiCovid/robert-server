package fr.gouv.tousantic.analytics.server.utils;

import fr.gouv.tousantic.analytics.server.controller.vo.TimestampedEventVo;
import fr.gouv.tousantic.analytics.server.model.kafka.TimestampedEvent;

public class TestUtils {

    public static TimestampedEvent convertTimestampedEvent(final TimestampedEventVo timestampedEventVo) {
        return TimestampedEvent.builder()
                .name(timestampedEventVo.getName())
                .timestamp(timestampedEventVo.getTimestamp())
                .description(timestampedEventVo.getDescription())
                .build();
    }

}
