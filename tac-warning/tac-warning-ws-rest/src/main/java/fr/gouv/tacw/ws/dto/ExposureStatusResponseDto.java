package fr.gouv.tacw.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExposureStatusResponseDto {
	private boolean atRisk;
}
