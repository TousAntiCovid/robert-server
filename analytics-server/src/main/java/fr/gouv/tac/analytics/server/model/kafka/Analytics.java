package fr.gouv.tac.analytics.server.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Analytics {

    private String installationUuid;

    private Map<String, String> infos;

    private List<TimestampedEvent> events;

    private List<TimestampedEvent> errors;

    private ZonedDateTime creationDate;
}
