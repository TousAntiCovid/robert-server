package fr.gouv.tacw.ws.vo.mapper;

import javax.validation.UnexpectedTypeException;

import org.springframework.stereotype.Component;

import fr.gouv.tacw.model.DynamicToken;
import fr.gouv.tacw.model.StaticToken;
import fr.gouv.tacw.model.Token;
import fr.gouv.tacw.ws.vo.VisitTokenVo;

@Component
public class TokenMapper {
	public Token getToken(VisitTokenVo visitTokenVo) {
		if (visitTokenVo.getType().isStatic()) {
			return new StaticToken(visitTokenVo.getPayload());
		} else if (visitTokenVo.getType().isDynamic()) {
			return new DynamicToken(visitTokenVo.getPayload());
		}
		throw new UnexpectedTypeException();
	}
}
