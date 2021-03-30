package fr.gouv.tousantic.analytics.server.controller.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ErrorVo {

    private String message;

    private ZonedDateTime timestamp;
}
