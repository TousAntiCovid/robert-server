package fr.gouv.tacw.ws.vo;

import lombok.Data;

@Data
public class QRcodeVo {
	public enum type { STATIC, DYNAMIC };
	private type qrType;
	private String qrCode;
	private String timestamp;
}
