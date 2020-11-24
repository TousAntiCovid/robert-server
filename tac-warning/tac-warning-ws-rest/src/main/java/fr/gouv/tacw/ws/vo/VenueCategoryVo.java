package fr.gouv.tacw.ws.vo;

/**
 * See https://www.service-public.fr/professionnels-entreprises/vosdroits/F32351 
 */
public enum VenueCategoryVo {
	CAT1("CAT1"), 
	CAT2("CAT2"), 
	CAT3("CAT3"), 
	CAT4("CAT4"), 
	CAT5("CAT5");

	private final String value;

	VenueCategoryVo(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
