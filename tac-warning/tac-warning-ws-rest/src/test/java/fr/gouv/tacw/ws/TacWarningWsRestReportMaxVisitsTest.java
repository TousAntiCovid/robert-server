package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.tacw.database.service.ExposedStaticVisitService;
import fr.gouv.tacw.ws.configuration.TacWarningWsRestConfiguration;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.service.TestTimestampService;
import fr.gouv.tacw.ws.service.impl.WarningServiceImpl;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.QRCodeVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.TokenTypeVo;
import fr.gouv.tacw.ws.vo.VenueCategoryVo;
import fr.gouv.tacw.ws.vo.VenueTypeVo;
import fr.gouv.tacw.ws.vo.VisitVo;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TacWarningWsRestReportMaxVisitsTest {
	@Value("${controller.path.prefix}" + UriConstants.API_V1)
	private String pathPrefixV1;

	@Autowired
	private TestRestTemplate restTemplate;
	
	@Autowired
	private TestTimestampService timestampService;
	
	@MockBean
	private ExposedStaticVisitService exposedStaticVisitService;
	
	@MockBean
	private TacWarningWsRestConfiguration configuration;
	
	private ListAppender<ILoggingEvent> warningServiceLoggerAppender;
	
	private KeyPair keyPair;
	
	@BeforeEach
	public void setUp() {
		keyPair = Keys.keyPairFor(AuthorizationService.algo);

		when(this.configuration.isJwtReportAuthorizationDisabled()).thenReturn(false);
		when(this.configuration.getRobertJwtPublicKey()).thenReturn(Encoders.BASE64.encode(keyPair.getPublic().getEncoded()));
	}
	
	@Test
	public void testCanReportWithMaxVisits() {
		this.setUpLogHandler();
		List<VisitVo> visitQrCodes = new ArrayList<VisitVo>(this.configuration.getMaxVisits());
		IntStream.rangeClosed(1, this.configuration.getMaxVisits())
				.forEach(i -> visitQrCodes.add(new VisitVo(timestampService.validTimestampString(),
						new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60,
								"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"))));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpJwtHeaderUtils(keyPair.getPrivate()).getReportEntityWithBearer(reportRequestVo), 
				String.class);
		
		verify(exposedStaticVisitService, times(this.configuration.getMaxVisits())).registerExposedStaticVisitEntities(anyList());
		assertThat(warningServiceLoggerAppender.list.size()).isEqualTo(1); // no second log with filter
		ILoggingEvent log = warningServiceLoggerAppender.list.get(0);
		assertThat(log.getMessage()).contains(
				String.format("Reporting %d visits", this.configuration.getMaxVisits()));
		assertThat(log.getLevel()).isEqualTo(Level.INFO);
	}
	
	@Test
	public void testCannotReportWithMoreThanMaxVisits() {
		this.setUpLogHandler();
		List<VisitVo> visitQrCodes = new ArrayList<VisitVo>(this.configuration.getMaxVisits());
		IntStream.rangeClosed(1, this.configuration.getMaxVisits() + 1)
				.forEach(i -> visitQrCodes.add(new VisitVo(timestampService.validTimestampString(),
						new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60,
								"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"))));
		ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);
		
		restTemplate.postForEntity(
				pathPrefixV1 + UriConstants.REPORT, 
				new HttpJwtHeaderUtils(keyPair.getPrivate()).getReportEntityWithBearer(reportRequestVo), 
				String.class);
	
		verify(exposedStaticVisitService, times(this.configuration.getMaxVisits())).registerExposedStaticVisitEntities(anyList());
		assertThat(warningServiceLoggerAppender.list.size()).isEqualTo(2); 
		ILoggingEvent log = warningServiceLoggerAppender.list.get(1);  // first log is nb visits for ESR
		assertThat(log.getMessage()).contains(
				String.format("Filtered %d visits out of %d", 1, this.configuration.getMaxVisits() + 1));
		assertThat(log.getLevel()).isEqualTo(Level.INFO);
	}

	private void setUpLogHandler() {
		Logger warningServiceLogger = (Logger) LoggerFactory.getLogger(WarningServiceImpl.class);
	
		warningServiceLoggerAppender = new ListAppender<>();
		warningServiceLoggerAppender.start();
		
		warningServiceLogger.addAppender(warningServiceLoggerAppender);
	}
	
}
