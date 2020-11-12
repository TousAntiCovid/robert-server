package fr.gouv.tacw.ws.dto;

import java.util.List;

import lombok.Data;

@Data
public class StatusResponseDto {
	private boolean warn;
	private List<String> tokensOfInterest;
}
