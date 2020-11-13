package fr.gouv.tacw.ws.service;

import fr.gouv.tacw.ws.vo.VisitTokenVo;

public class StaticVisitToken {

	public static StaticVisitToken fromVo(VisitTokenVo vo) {
		return new StaticVisitToken(vo.getPayload());
	}

	private final String payload;

	public StaticVisitToken(String payload) {
		this.payload = payload;
	}

	protected boolean isInfected() {
		return false;
	}
}
