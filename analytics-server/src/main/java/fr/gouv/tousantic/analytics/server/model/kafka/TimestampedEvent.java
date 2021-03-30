package fr.gouv.tousantic.analytics.server.model.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TimestampedEvent {

    private String name;

    private ZonedDateTime timestamp;

    private String description;
}
