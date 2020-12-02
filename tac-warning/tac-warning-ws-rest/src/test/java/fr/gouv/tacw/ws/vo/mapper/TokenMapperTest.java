package fr.gouv.tacw.ws.vo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.gouv.tacw.model.OpaqueDynamicVisit;
import fr.gouv.tacw.model.OpaqueStaticVisit;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;

public class TokenMapperTest {
	private TokenMapper tokenMapper;
	
	@BeforeEach
	public void setUp() {
		tokenMapper = new TokenMapper();
	}

	@Test
	public void testCanMapAStaticVisitTokenVo() {
		VisitTokenVo visitTokenVo = this.staticVisitTokenVoExample();

		OpaqueVisit token = tokenMapper.getToken(visitTokenVo);

		assertThat(token).isInstanceOf(OpaqueStaticVisit.class);
		assertThat(token.getPayload()).isEqualTo(visitTokenVo.getPayload());
	}

	@Test
	public void testCanMapADynamicVisitTokenVo() {
		VisitTokenVo visitTokenVo = this.dynamicVisitTokenVoExample();

		OpaqueVisit token = tokenMapper.getToken(visitTokenVo);

		assertThat(token).isInstanceOf(OpaqueDynamicVisit.class);
		assertThat(token.getPayload()).isEqualTo(visitTokenVo.getPayload());
	}

	private VisitTokenVo staticVisitTokenVoExample() {
		return new VisitTokenVo(TokenTypeVo.STATIC, "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", "1234");
	}

	private VisitTokenVo dynamicVisitTokenVoExample() {
		return new VisitTokenVo(TokenTypeVo.DYNAMIC, "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", "1234");
	}
}
