package fr.gouv.tacw.ws.vo;

import lombok.Data;

@Data
public class QRcodeVo {
	public enum type {
		STATIC, DYNAMIC
	};

	private String formatVersion;
	private type qrType;
	private String qrCode; // base64-encoded?
	private String timestamp;
}
