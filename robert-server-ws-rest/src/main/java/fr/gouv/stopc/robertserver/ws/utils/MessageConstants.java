package fr.gouv.stopc.robertserver.ws.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageConstants {

	SUCCESSFUL_OPERATION("Successful operation"),

	INVALID_AUTHENTICATION("Invalid authentication"),

	ERROR_OCCURED("An error occured"),

	INVALID_DATA("Invalid data");

	private String value;

}
