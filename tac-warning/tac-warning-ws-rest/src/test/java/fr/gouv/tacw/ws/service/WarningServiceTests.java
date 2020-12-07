package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
@ContextConfiguration(classes = { WarningServiceImpl.class, TokenMapper.class, ExposedStaticVisitServiceImpl.class, TestTimestampService.class})
@MockBean(ExposedStaticVisitRepository.class)
@MockBean(ExposedStaticVisitService.class)
@MockBean(ScoringProperties.class)
public class WarningServiceTests {
	@Autowired
	private ExposedStaticVisitService exposedStaticVisitService;

	@Autowired
	private WarningService warningService;

	@Autowired
	private TestTimestampService timestampService;
	
	@Captor
	ArgumentCaptor<List<ExposedStaticVisitEntity>> staticTokensCaptor;
	
	@Test
	public void testStatusOfVisitTokenNotInfectedIsNotAtRisk() {
		List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
		visits.add(new OpaqueStaticVisit("0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", timestampService.validTimestamp()));

		assertThat(warningService.getStatus(visits.stream(), 1L)).isFalse();
	}

	@Test
	public void testStatusOfVisitTokenInfectedIsAtRisk() {
		String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
		long visitTime = timestampService.validTimestamp();
		
		when(exposedStaticVisitService.riskScore(infectedToken, visitTime)).thenReturn(1L);
		List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
		visits.add(new OpaqueStaticVisit(infectedToken, visitTime));

		assertThat(warningService.getStatus(visits.stream(), 1L)).isTrue();
	}

	// TODO: test invalid timestamp in controller => reject whole request ? just the visit
	
	@Test
	public void testWhenStatusRequestWithVisitsHavingATimeInTheFutureThenVisitsAreFilteredOut() {
		String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
		long visitTime = timestampService.futureTimestamp();
		List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
		visits.add(new OpaqueStaticVisit(infectedToken, visitTime));

		warningService.getStatus(visits.stream(), 1L);
		
		verifyNoInteractions(exposedStaticVisitService);
	}
	
	@Test
	public void testWhenStatusRequestWithVisitsHavingATimeInThePastGreaterThanRetentionTimeThenVisitsAreFilteredOut() {
		String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
		long visitTime = timestampService.preRetentionTimeTimestamp();
		List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
		visits.add(new OpaqueStaticVisit(infectedToken, visitTime));

		warningService.getStatus(visits.stream(), 1L);
		
		verifyNoInteractions(exposedStaticVisitService);
	}
	
	@Test
	public void testWhenStatusRequestWithVisitsHavingATimeInThePastEqualsToRetentionTimeThenVisitIsNotFilteredOut() {
		String token = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
		long visitTime = timestampService.retentionTimeTimestamp();
		List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
		visits.add(new OpaqueStaticVisit(token, visitTime));

		warningService.getStatus(visits.stream(), 1L);
		
		verify(exposedStaticVisitService).riskScore(token, visitTime);
	}
	
	@Test
	public void testCanReportVisitsWhenInfected() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add(new VisitVo(timestampService.validTimestampString(), 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));
		visits.add(new VisitVo(timestampService.validTimestampString(), 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

		warningService.reportVisitsWhenInfected(new ReportRequestVo(visits));
		verify(exposedStaticVisitService, times(2)).registerOrIncrementExposedStaticVisits(staticTokensCaptor.capture());
		assertThat(staticTokensCaptor.getValue().size()).isEqualTo(ExposedTokenGenerator.numberOfGeneratedTokens());
	}

	@Test
	public void testWhenReportingExposedVisitsThenVisitsHavingATimeInTheFutureAreFilteredOut() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add(new VisitVo(timestampService.futureTimestampString(), 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));
		visits.add(new VisitVo(timestampService.validTimestampString(), 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

		warningService.reportVisitsWhenInfected(new ReportRequestVo(visits));
		verify(exposedStaticVisitService, times(1)).registerOrIncrementExposedStaticVisits(staticTokensCaptor.capture());
		assertThat(staticTokensCaptor.getValue().size()).isEqualTo(ExposedTokenGenerator.numberOfGeneratedTokens());
	}

	@Test
	public void testWhenReportingExposedVisitsThenVisitsHavingATimeInThePastGreaterThanRetentionTimeAreFilteredOut() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add(new VisitVo(timestampService.preRetentionTimeTimestampString(), 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));
		visits.add(new VisitVo(timestampService.validTimestampString(), 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

		warningService.reportVisitsWhenInfected(new ReportRequestVo(visits));
		verify(exposedStaticVisitService, times(1)).registerOrIncrementExposedStaticVisits(staticTokensCaptor.capture());
		assertThat(staticTokensCaptor.getValue().size()).isEqualTo(ExposedTokenGenerator.numberOfGeneratedTokens());
	}
	
	@Test
	public void testWhenReportingExposedVisitsThenVisitsHavingInvalidTimestampAreFilteredOut() {
		List<VisitVo> visits = new ArrayList<VisitVo>();
		visits.add(new VisitVo("foo", 
				new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

		warningService.reportVisitsWhenInfected(new ReportRequestVo(visits));
		
		verifyNoInteractions(exposedStaticVisitService);
	}
	
}
