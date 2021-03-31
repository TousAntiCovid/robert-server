package fr.gouv.tac.analytics.server.model.kafka;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TimestampedEvent {

    private String name;

    private ZonedDateTime timestamp;

    private String description;
}
