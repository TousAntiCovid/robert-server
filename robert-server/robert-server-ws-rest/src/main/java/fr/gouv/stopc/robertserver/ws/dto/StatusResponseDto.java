package fr.gouv.stopc.robertserver.ws.dto;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StatusResponseDto {
    @NotNull
    private RiskLevel riskLevel;

    @NotNull
    private String tuples;

    @Singular("config")
    private List<ClientConfigDto> config;

    private String message;

    private String lastContactDate;
}
