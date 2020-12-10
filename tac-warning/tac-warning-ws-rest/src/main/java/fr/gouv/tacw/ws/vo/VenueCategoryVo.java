package fr.gouv.tacw.ws.vo;

/**
 * See https://www.service-public.fr/professionnels-entreprises/vosdroits/F32351 
 */
public enum VenueCategoryVo {
	CAT1(1), 
	CAT2(2), 
	CAT3(3), 
	CAT4(4), 
	CAT5(5);

	private final int value;

	VenueCategoryVo(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
