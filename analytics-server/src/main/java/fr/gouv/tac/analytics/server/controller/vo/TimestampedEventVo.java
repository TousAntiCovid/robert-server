package fr.gouv.tac.analytics.server.controller.vo;

import java.time.ZonedDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
