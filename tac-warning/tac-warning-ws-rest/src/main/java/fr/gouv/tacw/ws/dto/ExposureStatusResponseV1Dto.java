package fr.gouv.tacw.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExposureStatusResponseV1Dto {
	private boolean atRisk;
}
