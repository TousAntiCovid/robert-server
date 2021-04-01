package fr.gouv.stopc.robertserver.ws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusResponseDto {
    @NotNull
    private RiskLevel riskLevel;

    @NotNull
    private String tuples;

    @Singular("config")
    private List<ClientConfigDto> config;

    private String message;

    private String lastContactDate;

    private String lastRiskScoringDate;

    private String declarationToken;

    private String analyticsToken;

}
