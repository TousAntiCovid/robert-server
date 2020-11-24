package fr.gouv.tacw.ws.vo;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QRCodeVo {
	@NotNull
	@Valid
	private TokenTypeVo type;
	private String venueType;
	@Valid
	private VenueCategoryVo venueCategory;
	private int venueCapacity;
	@NotNull
	private String uuid;
}
