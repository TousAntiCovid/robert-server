package fr.gouv.tacw.model;

import fr.gouv.tacw.ws.service.ExposureStatusService;

public class DynamicToken extends Token {
	public DynamicToken(String payload) {
		super(payload);
	}

	public boolean isExposed(ExposureStatusService exposureStatusService) {
		return exposureStatusService.isExposed(this);
	}
}
