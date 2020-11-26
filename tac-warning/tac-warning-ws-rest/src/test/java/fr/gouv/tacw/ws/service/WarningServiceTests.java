package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import fr.gouv.tacw.database.model.ExposedStaticVisitTokenEntity;
import fr.gouv.tacw.database.service.TokenService;
import fr.gouv.tacw.database.service.TokenServiceImpl;
import fr.gouv.tacw.model.ExposedTokenGenerator;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.mapper.TokenMapper;


@SpringBootTest(classes = { WarningServiceImpl.class, TokenServiceImpl.class, TokenMapper.class, ExposureStatusServiceImpl.class })
@MockBean(TokenService.class)
public class WarningServiceTests {
	@Autowired
	private TokenService tokenService;

	@Autowired
	private WarningService warningService;

	@Captor
	ArgumentCaptor<List<ExposedStaticVisitTokenEntity>> staticTokensCaptor;
	
	@Test
	public void testStatusOfVisitTokenNotInfectedIsNotAtRisk() {
		List<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"));

		ExposureStatusRequestVo statusRequestVo = new ExposureStatusRequestVo(visitTokens);

		assertThat(warningService.getStatus(statusRequestVo)).isFalse();
	}

	@Test
	public void testStatusOfVisitTokenInfectedIsAtRisk() {
		String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
		when(tokenService.exposedStaticTokensIncludes(infectedToken)).thenReturn(true);
		List<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(TokenTypeVo.STATIC, infectedToken));

		ExposureStatusRequestVo statusRequestVo = new ExposureStatusRequestVo(visitTokens);

		assertThat(warningService.getStatus(statusRequestVo)).isTrue();
	}

	@Test
	public void testCanReportVisitsWhenInfected() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add(new VisitVo("12345", 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));


		warningService.reportVisitsWhenInfected(new ReportRequestVo(visits));
		verify(tokenService).registerExposedStaticTokens(staticTokensCaptor.capture());
		assertThat(staticTokensCaptor.getValue().size()).isEqualTo(ExposedTokenGenerator.numberOfGeneratedTokens());
	}
}
