package fr.gouv.tacw.ws.dto;

import fr.gouv.tacw.database.model.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExposureStatusResponseDto {
	private RiskLevel riskLevel;
	private String lastContactDate;
}
