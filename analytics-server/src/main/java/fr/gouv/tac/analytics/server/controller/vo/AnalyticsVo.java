package fr.gouv.tac.analytics.server.controller.vo;


import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import fr.gouv.tac.analytics.server.config.validation.validator.AnalyticsVoInfoSize;
import fr.gouv.tac.analytics.server.config.validation.validator.TimestampedEventCollection;
import fr.gouv.tac.analytics.server.config.validation.validator.TimestampedEventCollectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
