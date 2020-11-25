package fr.gouv.tacw.ws.vo;

/**
 * See https://www.service-public.fr/professionnels-entreprises/vosdroits/F32351
 */
public enum VenueTypeVo {
	J("J"), 
	L("L"), 
	M("M"),
	N("N"),
	O("O"), 
	P("P"),
	R("R"),
	S("S"),
	T("T"),
	U("U"),
	V("V"),
	W("W"),
	X("X"),
	Y("Y"),
	PA("PA"),
	SG("SG"),
	PS("PS"),
	GA("GA"),
	OA("OA"),
	REF("REF");
	
	private final String value;

	VenueTypeVo(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
