package fr.gouv.tacw.model;

import fr.gouv.tacw.ws.service.ExposureStatusService;
import lombok.Data;

@Data
public abstract class Token {
	private final String payload;

	public Token(String payload) {
		this.payload = payload;
	};
	
	public abstract boolean isExposed(ExposureStatusService exposureStatusService);
}
