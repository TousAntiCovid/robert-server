package fr.gouv.tousantic.analytics.server.controller.mapper;

import fr.gouv.tousantic.analytics.server.controller.vo.AnalyticsVo;
import fr.gouv.tousantic.analytics.server.model.kafka.Analytics;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;


@Mapper(componentModel = "spring", uses = TimestampedEventMapper.class)
public interface AnalyticsMapper {


    default Optional<Analytics> map(final AnalyticsVo analyticsVo) {
        return Optional.ofNullable(this.mapInternal(analyticsVo));
    }

    @Mapping(target = "creationDate", expression = "java( java.time.ZonedDateTime.now() )")
    Analytics mapInternal(final AnalyticsVo analyticsVo);

}
