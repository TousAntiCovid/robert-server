package fr.gouv.tacw.ws.dto;

import lombok.Data;

@Data
public class StatusResponseDto {
	public enum code { OK, KO, MORE };
	private boolean warn;
	private code returnCode;
}
