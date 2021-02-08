package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.tacw.database.model.ExposedStaticVisitEntity;
import fr.gouv.tacw.database.model.RiskLevel;
import fr.gouv.tacw.database.model.ScoreResult;
import fr.gouv.tacw.database.repository.ExposedStaticVisitRepository;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.database.service.ExposedStaticVisitServiceImpl;
import fr.gouv.tacw.database.utils.TimeUtils;
import fr.gouv.tacw.model.OpaqueStaticVisit;
import fr.gouv.tacw.model.OpaqueVisit;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.service.impl.ExposedTokenGeneratorServiceImpl;
import fr.gouv.tacw.ws.service.impl.ScoringServiceImpl;
import fr.gouv.tacw.ws.service.impl.WarningServiceImpl;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import fr.gouv.tacw.ws.vo.mapper.TokenMapper;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { WarningServiceImpl.class, TokenMapper.class, ExposedStaticVisitServiceImpl.class, TestTimestampService.class, ExposedTokenGeneratorServiceImpl.class, ScoringServiceImpl.class})
@MockBean(ExposedStaticVisitRepository.class)
@EnableConfigurationProperties(value = TacWarningWsRestConfiguration.class)
@TestPropertySource("classpath:application.properties")
public class WarningServiceTests {
    @MockBean
    private ExposedStaticVisitService exposedStaticVisitService;

    @Autowired
    private WarningService warningService;

    @Autowired
    private TestTimestampService timestampService;

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private ExposedTokenGeneratorServiceImpl tokenGeneratorService;

    @Captor
    ArgumentCaptor<List<ExposedStaticVisitEntity>> staticTokensCaptor;

    private int numberOfGeneratedTokens;

    @BeforeEach
    public void setUp() {
        numberOfGeneratedTokens = tokenGeneratorService.numberOfGeneratedTokens();
    }

    @Test
    public void testStatusOfVisitTokenNotInfectedIsNotAtRisk() {
        List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
        visits.add(new OpaqueStaticVisit("0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc", timestampService.validTimestamp()));

        assertThat(warningService.getStatus(visits.stream()).getRiskLevel()).isEqualTo(RiskLevel.NONE);
    }

    @Test
    public void testStatusOfVisitTokenInfectedIsAtRisk() {
        String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
        long visitTime = timestampService.validTimestamp();
        long increment = scoringService.getScoreIncrement(VenueTypeVo.N);
        List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();

        when(exposedStaticVisitService.riskScore(infectedToken, visitTime))
            .thenReturn( Arrays.asList(new ScoreResult(RiskLevel.HIGH, increment, visitTime)) );
        visits.add(new OpaqueStaticVisit(infectedToken, visitTime));

        assertThat(warningService.getStatus(visits.stream()).getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(warningService.getStatus(visits.stream()).getLastContactDate())
            .isEqualTo(Instant.ofEpochSecond(visitTime).truncatedTo(ChronoUnit.DAYS).getEpochSecond());
    }

    @Test
    public void testStatusWhenManyInfectedVisitsWithVariousRiskLevelsThenRiskLevelIsTheHighestRiskOverThreshold() {
        String infectedTokenN = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
        String infectedTokenPEV = "FDJHFJDZTR7847UT5DU4DEJ8D38HSK99FLNDKHGFJDFIIUGQVNVUYRc";
        long visitTime = timestampService.validTimestamp();
        long incrementN = scoringService.getScoreIncrement(VenueTypeVo.N);
        long incrementPEV = scoringService.getScoreIncrement(VenueTypeVo.PEV);
        List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();

        when(exposedStaticVisitService.riskScore(infectedTokenN, visitTime))
            .thenReturn( Arrays.asList(new ScoreResult(RiskLevel.LOW, incrementN, visitTime)) );
        when(exposedStaticVisitService.riskScore(infectedTokenPEV, visitTime))
        .thenReturn( Arrays.asList(new ScoreResult(RiskLevel.HIGH, incrementPEV, visitTime)) );
        IntStream.range(1, 200).forEach(i -> visits.add(new OpaqueStaticVisit(infectedTokenN, visitTime)));
        visits.add(new OpaqueStaticVisit(infectedTokenPEV, visitTime));
        
        assertThat(warningService.getStatus(visits.stream()).getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(warningService.getStatus(visits.stream()).getLastContactDate())
            .isEqualTo(Instant.ofEpochSecond(visitTime).truncatedTo(ChronoUnit.DAYS).getEpochSecond());
    }
    
    @Test
    public void testStatusWhenManyVisitsThenRiskLevelIsTheLastInfectedVisit() {
        String infectedToken = "infected";
        String healthyToken = "healthy";
        long visitTimeInfected = timestampService.validTimestamp();
        long visitTime = visitTimeInfected + TimeUtils.TIME_ROUNDING * 20;
        long increment = scoringService.getScoreIncrement(VenueTypeVo.N);
        List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
        
        when(exposedStaticVisitService.riskScore(infectedToken, visitTimeInfected))
            .thenReturn( Arrays.asList(new ScoreResult(RiskLevel.LOW, increment, visitTimeInfected)) );
        when(exposedStaticVisitService.riskScore(healthyToken, visitTime))
            .thenReturn( Collections.emptyList() );
        visits.add(new OpaqueStaticVisit(infectedToken, visitTimeInfected - TimeUtils.TIME_ROUNDING * 96));
        visits.add(new OpaqueStaticVisit(infectedToken, visitTimeInfected));
        visits.add(new OpaqueStaticVisit(infectedToken, visitTimeInfected - TimeUtils.TIME_ROUNDING * 192 ));
        visits.add(new OpaqueStaticVisit(healthyToken, visitTime));
        
        assertThat(warningService.getStatus(visits.stream()).getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(warningService.getStatus(visits.stream()).getLastContactDate())
            .isEqualTo(Instant.ofEpochSecond(visitTimeInfected).truncatedTo(ChronoUnit.DAYS).getEpochSecond());
    }
    
    @Disabled("Temporary removed timestamp present/future checking")
    @Test
    public void testWhenStatusRequestWithVisitsHavingATimeInTheFutureThenVisitsAreFilteredOut() {
        String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
        long visitTime = timestampService.futureTimestamp();
        List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
        visits.add(new OpaqueStaticVisit(infectedToken, visitTime));

        warningService.getStatus(visits.stream());

        verify(exposedStaticVisitService, times(1)).registerExposedStaticVisitEntities(staticTokensCaptor.capture());
        assertThat(staticTokensCaptor.getValue()).isEmpty();
    }

    @Disabled("Temporary removed timestamp present/future checking")
    @Test
    public void testWhenStatusRequestWithVisitsHavingATimeInThePastGreaterThanRetentionTimeThenVisitsAreFilteredOut() {
        String infectedToken = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
        long visitTime = timestampService.preRetentionTimeTimestamp();
        List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
        visits.add(new OpaqueStaticVisit(infectedToken, visitTime));

        warningService.getStatus(visits.stream());

        verify(exposedStaticVisitService, times(1)).registerExposedStaticVisitEntities(staticTokensCaptor.capture());
        assertThat(staticTokensCaptor.getValue()).isEmpty();
    }

    @Test
    public void testWhenStatusRequestWithVisitsHavingATimeInThePastEqualsToRetentionTimeThenVisitIsNotFilteredOut() {
        String token = "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc";
        long visitTime = timestampService.retentionTimeTimestamp();
        List<OpaqueVisit> visits = new ArrayList<OpaqueVisit>();
        visits.add(new OpaqueStaticVisit(token, visitTime));

        warningService.getStatus(visits.stream());

        verify(exposedStaticVisitService).riskScore(token, visitTime);
    }

    @Test
    public void testCanReportVisitsWhenInfected() {
        List<VisitVo> visits = new ArrayList<VisitVo>();
        visits.add(new VisitVo(timestampService.validTimestampString(), 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));
        visits.add(new VisitVo(timestampService.validTimestampString(), 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

        warningService.reportVisitsWhenInfected(visits);
        verify(exposedStaticVisitService, times(2)).registerExposedStaticVisitEntities(staticTokensCaptor.capture());
        assertThat(staticTokensCaptor.getValue().size()).isEqualTo(this.numberOfGeneratedTokens);
    }

    @Disabled("Temporary removed timestamp present/future checking")
    @Test
    public void testWhenReportingExposedVisitsThenVisitsHavingATimeInTheFutureAreFilteredOut() {
        List<VisitVo> visits = new ArrayList<VisitVo>();
        visits.add(new VisitVo(timestampService.futureTimestampString(), 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));
        visits.add(new VisitVo(timestampService.validTimestampString(), 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

        warningService.reportVisitsWhenInfected(visits);
        verify(exposedStaticVisitService).registerExposedStaticVisitEntities(staticTokensCaptor.capture());
        assertThat(staticTokensCaptor.getValue().size()).isEqualTo(this.numberOfGeneratedTokens);
    }

    @Disabled("Temporary removed timestamp present/future checking")
    @Test
    public void testWhenReportingExposedVisitsThenVisitsHavingATimeInThePastGreaterThanRetentionTimeAreFilteredOut() {
        List<VisitVo> visits = new ArrayList<VisitVo>();
        visits.add(new VisitVo(timestampService.preRetentionTimeTimestampString(), 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));
        visits.add(new VisitVo(timestampService.validTimestampString(), 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

        warningService.reportVisitsWhenInfected(visits);
        verify(exposedStaticVisitService).registerExposedStaticVisitEntities(staticTokensCaptor.capture());
        assertThat(staticTokensCaptor.getValue().size()).isEqualTo(this.numberOfGeneratedTokens);
    }

    @Test
    public void testWhenReportingExposedVisitsThenVisitsHavingInvalidTimestampAreFilteredOut() {
        List<VisitVo> visits = new ArrayList<VisitVo>();
        visits.add(new VisitVo("foo", 
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60, "UUID")));

        warningService.reportVisitsWhenInfected(visits);

        verifyNoInteractions(exposedStaticVisitService);
    }

}
