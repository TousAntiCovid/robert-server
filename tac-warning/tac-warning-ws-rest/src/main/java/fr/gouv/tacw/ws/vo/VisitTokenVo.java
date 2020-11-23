package fr.gouv.tacw.ws.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VisitTokenVo {
	private TokenTypeVo type;
	private String payload;
}
