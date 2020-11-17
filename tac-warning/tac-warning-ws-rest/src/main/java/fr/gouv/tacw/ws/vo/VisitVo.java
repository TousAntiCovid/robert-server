package fr.gouv.tacw.ws.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VisitVo {
	private String timestamp;
	private QRCodeVo qrCode;
}
