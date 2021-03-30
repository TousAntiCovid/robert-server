package fr.gouv.tousantic.analytics.server.controller.mapper;

import fr.gouv.tousantic.analytics.server.model.kafka.TimestampedEvent;
import fr.gouv.tousantic.analytics.server.controller.vo.TimestampedEventVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimestampedEventMapper {

    TimestampedEvent map(TimestampedEventVo timestampedEventVo);

}


