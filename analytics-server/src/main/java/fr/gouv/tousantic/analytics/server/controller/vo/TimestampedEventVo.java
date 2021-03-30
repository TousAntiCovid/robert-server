package fr.gouv.tousantic.analytics.server.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TimestampedEventVo {

    @NotBlank
    private String name;

    @NotNull
    private ZonedDateTime timestamp;

    private String description;
}
