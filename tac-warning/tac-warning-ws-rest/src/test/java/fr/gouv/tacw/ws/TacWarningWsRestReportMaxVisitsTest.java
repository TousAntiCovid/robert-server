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
import fr.gouv.tacw.ws.controller.TACWarningController;
import fr.gouv.tacw.ws.service.AuthorizationService;
import fr.gouv.tacw.ws.service.TestTimestampService;
import fr.gouv.tacw.ws.service.impl.WarningServiceImpl;
import fr.gouv.tacw.ws.utils.BadArgumentsLoggerService;
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
    @Value("${controller.path.prefix}" + UriConstants.API_V2)
    private String pathPrefixV2;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestTimestampService timestampService;

    @MockBean
    private ExposedStaticVisitService exposedStaticVisitService;

    @MockBean
    private TacWarningWsRestConfiguration configuration;

    private ListAppender<ILoggingEvent> warningServiceLoggerAppender;

    private ListAppender<ILoggingEvent> badArgumentLoggerAppender;

    private ListAppender<ILoggingEvent> tacWarningControllerLoggerAppender;

    private KeyPair keyPair;

    @Test
    public void testCanReportWithMaxVisits() {
        List<VisitVo> visitQrCodes = new ArrayList<VisitVo>(this.configuration.getMaxVisits());
        IntStream.rangeClosed(1, this.configuration.getMaxVisits())
        .forEach(i -> visitQrCodes.add(new VisitVo(timestampService.validTimestampString(),
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60,
                        "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"))));
        ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);

        restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.REPORT, 
                new HttpJwtHeaderUtils(keyPair.getPrivate()).getReportEntityWithBearer(reportRequestVo), 
                String.class);

        verify(exposedStaticVisitService, times(this.configuration.getMaxVisits())).registerExposedStaticVisitEntities(anyList());
        assertThat(tacWarningControllerLoggerAppender.list.size()).isEqualTo(1);
        ILoggingEvent log = tacWarningControllerLoggerAppender.list.get(0);
        assertThat(log.getMessage()).contains(
                String.format(TACWarningController.REPORT_LOG_MESSAGE, this.configuration.getMaxVisits()));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(warningServiceLoggerAppender.list)
            .filteredOn(logEvent -> ! logEvent.getMessage().startsWith("Adding exposed visit"))
            .isEmpty(); // no log with filter
        assertThat(badArgumentLoggerAppender.list).isEmpty();
    }

    @Test
    public void testCannotReportWithMoreThanMaxVisits() {
        List<VisitVo> visitQrCodes = new ArrayList<VisitVo>(this.configuration.getMaxVisits());
        int nbVisits = this.configuration.getMaxVisits() + 1;
        IntStream.rangeClosed(1, nbVisits)
            .forEach(i -> visitQrCodes.add(new VisitVo(timestampService.validTimestampString(),
                new QRCodeVo(TokenTypeVo.STATIC, VenueTypeVo.N, VenueCategoryVo.CAT1, 60,
                        "0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"))));
        ReportRequestVo reportRequestVo = new ReportRequestVo(visitQrCodes);

        restTemplate.postForEntity(
                pathPrefixV2 + UriConstants.REPORT, 
                new HttpJwtHeaderUtils(keyPair.getPrivate()).getReportEntityWithBearer(reportRequestVo), 
                String.class);

        verify(exposedStaticVisitService, times(this.configuration.getMaxVisits())).registerExposedStaticVisitEntities(anyList());
        assertThat(tacWarningControllerLoggerAppender.list.size()).isEqualTo(1);
        ILoggingEvent log = tacWarningControllerLoggerAppender.list.get(0);
        assertThat(log.getMessage()).contains(
                String.format(TACWarningController.REPORT_LOG_MESSAGE, nbVisits));
        assertThat(log.getLevel()).isEqualTo(Level.INFO);
        assertThat(warningServiceLoggerAppender.list.size()).isEqualTo(31); 
        ILoggingEvent wlog = warningServiceLoggerAppender.list.get(0);  // filter log
        assertThat(wlog.getMessage()).contains(
                String.format(WarningServiceImpl.REPORT_MAX_VISITS_FILTER_LOG_MESSAGE, 1, nbVisits));
        assertThat(wlog.getLevel()).isEqualTo(Level.INFO);
        assertThat(badArgumentLoggerAppender.list).isEmpty();
    }

    @BeforeEach
    public void setUp() {
        keyPair = Keys.keyPairFor(AuthorizationService.algo);

        when(this.configuration.isJwtReportAuthorizationDisabled()).thenReturn(false);
        when(this.configuration.getRobertJwtPublicKey()).thenReturn(Encoders.BASE64.encode(keyPair.getPublic().getEncoded()));
        when(this.configuration.getMaxVisits()).thenReturn(30);

        this.setUpLogHandler();
    }

    private void setUpLogHandler() {
        this.warningServiceLoggerAppender = new ListAppender<>();
        this.warningServiceLoggerAppender.start();
        ((Logger) LoggerFactory.getLogger(WarningServiceImpl.class)).addAppender(this.warningServiceLoggerAppender);

        this.tacWarningControllerLoggerAppender = new ListAppender<>();
        this.tacWarningControllerLoggerAppender.start();
        ((Logger) LoggerFactory.getLogger(TACWarningController.class)).addAppender(this.tacWarningControllerLoggerAppender);

        this.badArgumentLoggerAppender = new ListAppender<>();
        this.badArgumentLoggerAppender.start();
        ((Logger) LoggerFactory.getLogger(BadArgumentsLoggerService.class)).addAppender(this.badArgumentLoggerAppender);
    }

}
