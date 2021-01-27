package fr.gouv.tousantic.robert.analytics.server.controller.mapper;

import fr.gouv.tousantic.robert.analytics.server.controller.vo.TimestampedEventVo;
import fr.gouv.tousantic.robert.analytics.server.model.kafka.TimestampedEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimestampedEventMapper {

    TimestampedEvent map(TimestampedEventVo timestampedEventVo);

}


