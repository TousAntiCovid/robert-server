package fr.gouv.tacw.ws.dto;

import java.util.Collections;
import java.util.List;

import fr.gouv.tacw.ws.vo.VisitTokenVo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExposureStatusResponseDto {
	private boolean atRisk;
	private List<VisitTokenVo> tokensOfInterest;
	
	public ExposureStatusResponseDto(boolean atRisk) {
		this.atRisk = atRisk;
		this.tokensOfInterest = Collections.emptyList();
	}
}
