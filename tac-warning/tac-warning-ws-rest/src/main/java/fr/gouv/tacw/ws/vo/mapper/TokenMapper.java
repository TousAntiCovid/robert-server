package fr.gouv.tacw.ws.vo.mapper;

import javax.validation.UnexpectedTypeException;

import org.springframework.stereotype.Component;

import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.model.OpaqueDynamicVisit;
import fr.gouv.tacw.model.OpaqueStaticVisit;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.vo.VisitTokenVo;

@Component
public class TokenMapper {
	public OpaqueVisit getToken(VisitTokenVo visitTokenVo) {
		final long visitTime = TimeUtils.roundedTimestamp(Long.parseLong(visitTokenVo.getTimestamp()));
		
		if (visitTokenVo.getType().isStatic()) {
			return new OpaqueStaticVisit(visitTokenVo.getPayload(), visitTime);
		} else if (visitTokenVo.getType().isDynamic()) {
			return new OpaqueDynamicVisit(visitTokenVo.getPayload(), visitTime);
		}
		throw new UnexpectedTypeException();
	}
}
