package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.database.service.ExposedStaticVisitServiceImpl;
import fr.gouv.tacw.model.ExposedTokenGenerator;
import fr.gouv.tacw.model.OpaqueStaticVisit;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.properties.ScoringProperties;
import fr.gouv.tacw.ws.service.impl.WarningServiceImpl;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.mapper.TokenMapper;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { WarningServiceImpl.class, TokenMapper.class, ExposedStaticVisitServiceImpl.class})
@MockBean(ExposedStaticVisitRepository.class)
@MockBean(ExposedStaticVisitService.class)
@MockBean(ScoringProperties.class)
public class WarningServiceTests {
	@Autowired
	private ExposedStaticVisitService exposedStaticVisitService;

	@Autowired
	private WarningService warningService;

	@Captor
	ArgumentCaptor<List<ExposedStaticVisitEntity>> staticTokensCaptor;
	
	@Test
	public void testStatusOfVisitTokenNotInfectedIsNotAtRisk() {
		List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
		visits.add(new OpaqueStaticVisit("0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", 123456789L));

		assertThat(warningService.getStatus(visits.stream(), 1L)).isFalse();
	}

	@Test
	public void testStatusOfVisitTokenInfectedIsAtRisk() {
		String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
		long visitTime = 12346789L;
		
		when(exposedStaticVisitService.riskScore(infectedToken, visitTime)).thenReturn(1L);
		List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
		visits.add(new OpaqueStaticVisit(infectedToken, visitTime));

		assertThat(warningService.getStatus(visits.stream(), 1L)).isTrue();
	}

	@Test
	public void testCanReportVisitsWhenInfected() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add(new VisitVo("12345", 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));


		warningService.reportVisitsWhenInfected(new ReportRequestVo(visits));
		verify(exposedStaticVisitService).registerOrIncrementExposedStaticVisits(staticTokensCaptor.capture());
		assertThat(staticTokensCaptor.getValue().size()).isEqualTo(ExposedTokenGenerator.numberOfGeneratedTokens());
	}
}
