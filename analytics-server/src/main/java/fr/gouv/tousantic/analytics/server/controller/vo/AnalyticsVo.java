package fr.gouv.tousantic.analytics.server.controller.vo;


import fr.gouv.tousantic.analytics.server.config.validation.validator.AnalyticsVoInfoSize;
import fr.gouv.tousantic.analytics.server.config.validation.validator.TimestampedEventCollection;
import fr.gouv.tousantic.analytics.server.config.validation.validator.TimestampedEventCollectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AnalyticsVo {

    @NotBlank
    @Size(max = 64)
    private String installationUuid;

    @AnalyticsVoInfoSize
    private Map<String, String> infos;

    @Valid
    @TimestampedEventCollection(type = TimestampedEventCollectionType.EVENT)
    private List<TimestampedEventVo> events;

    @Valid
    @TimestampedEventCollection(type = TimestampedEventCollectionType.ERROR)
    private List<TimestampedEventVo> errors;
}
