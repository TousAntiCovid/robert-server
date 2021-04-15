package fr.gouv.tac.analytics.server.utils;

import fr.gouv.tac.analytics.server.controller.vo.TimestampedEventVo;
import fr.gouv.tac.analytics.server.model.kafka.TimestampedEvent;

public class TestUtils {

    public static TimestampedEvent convertTimestampedEvent(final TimestampedEventVo timestampedEventVo) {
        return TimestampedEvent.builder()
                .name(timestampedEventVo.getName())
                .timestamp(timestampedEventVo.getTimestamp())
                .desc(timestampedEventVo.getDesc())
                .build();
    }

}
