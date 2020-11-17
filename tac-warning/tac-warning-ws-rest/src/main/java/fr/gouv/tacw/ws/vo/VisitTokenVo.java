package fr.gouv.tacw.ws.vo;

import fr.gouv.tacw.ws.service.TokenType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VisitTokenVo {
	private TokenType type;
	private String payload;
}
