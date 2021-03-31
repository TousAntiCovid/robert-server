package fr.gouv.tac.analytics.server.controller.mapper;

import fr.gouv.tac.analytics.server.model.kafka.TimestampedEvent;
import fr.gouv.tac.analytics.server.controller.vo.TimestampedEventVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimestampedEventMapper {

    TimestampedEvent map(TimestampedEventVo timestampedEventVo);

}


