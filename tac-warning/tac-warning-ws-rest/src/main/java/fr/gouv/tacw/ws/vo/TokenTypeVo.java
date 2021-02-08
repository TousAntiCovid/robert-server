package fr.gouv.tacw.ws.vo;

public enum TokenTypeVo {
	STATIC("static"), 
	DYNAMIC("dynamic");

	private final String value;

	TokenTypeVo(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	public boolean isStatic() {
		return this == STATIC;
	}

	public boolean isDynamic() {
		return this == DYNAMIC;
	}
}