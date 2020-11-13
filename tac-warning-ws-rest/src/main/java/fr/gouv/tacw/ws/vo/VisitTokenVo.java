package fr.gouv.tacw.ws.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitTokenVo {
	public enum tokenType {
		STATIC, DYNAMIC
	};

	private tokenType type;
	private String payload;

	public boolean isStatic() {
		return type.equals(tokenType.STATIC);
	}
}
