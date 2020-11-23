package fr.gouv.tacw.ws.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QRCodeVo {
	private TokenTypeVo type;
	private String venueType;
	private int venueCapacity;
	private String uuid;
}
