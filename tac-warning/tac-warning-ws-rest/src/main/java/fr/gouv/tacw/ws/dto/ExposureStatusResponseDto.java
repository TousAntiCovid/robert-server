package fr.gouv.tacw.ws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.gouv.tacw.database.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExposureStatusResponseDto {
    private RiskLevel riskLevel;
    private String lastContactDate;
}
