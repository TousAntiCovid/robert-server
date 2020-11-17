package fr.gouv.tacw.ws.service;

import java.util.Optional;


public enum TokenType {
	STATIC("static"), 
	DYNAMIC("dynamic");

	private final String value;

	public static Optional<TokenType> fromValue(String text) {
		for (TokenType tokenType : TokenType.values()) {
			if (String.valueOf(tokenType.value).equals(text)) {
				return Optional.of(tokenType);
			}
		}
		return Optional.empty();
	}
	TokenType(String value) {
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