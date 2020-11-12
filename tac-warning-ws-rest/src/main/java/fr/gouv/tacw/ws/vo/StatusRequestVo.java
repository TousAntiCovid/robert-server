package fr.gouv.tacw.ws.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatusRequestVo {
	private List<QRcodeVo> qrcodes;
	private List<TokenVo> tokens;
}
