package fr.gouv.tacw.model;

import lombok.Data;

/**
 * An opaque visit is a Visit with the minimum information needed to check the exposure status.
 * It is used when doing an Exposure Status Request. 
 */
@Data
public abstract class OpaqueVisit {
	private final String payload;
	private final long visitTime;

	public OpaqueVisit(String payload, long visitTime) {
		this.payload = payload;
		this.visitTime = visitTime;
	};
}
