package fr.gouv.tacw.ws.vo;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VisitVo {
	@NotNull
	private String timestamp;
	@NotNull
	@Valid
	private QRCodeVo qrCode;
}
