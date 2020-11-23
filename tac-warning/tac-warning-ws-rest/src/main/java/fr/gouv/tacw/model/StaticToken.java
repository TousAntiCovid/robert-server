package fr.gouv.tacw.model;

import fr.gouv.tacw.ws.service.ExposureStatusService;

public class StaticToken extends Token {

	public StaticToken(String payload) {
		super(payload);
	}

	public boolean isExposed(ExposureStatusService exposureStatusService) {
		return exposureStatusService.isExposed(this);
	}
}
