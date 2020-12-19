package fr.gouv.tacw.ws.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class TacWarningWsExceptionHandlingTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Value("${controller.path.prefix}" + UriConstants.API_V2)
	private String pathPrefixV2;

	@MockBean
	WarningService warningService;

	private Logger customRestExceptionHandlerLogger;

	private ListAppender<ILoggingEvent> listAppender;

	private String statusUrl;

	@BeforeEach
	private void setUp() {
		statusUrl = pathPrefixV2 + UriConstants.STATUS;
		customRestExceptionHandlerLogger = (Logger) LoggerFactory.getLogger(CustomRestExceptionHandler.class);

		listAppender = new ListAppender<>();
		listAppender.start();

		customRestExceptionHandlerLogger.addAppender(listAppender);
	}
	
    @Test
    public void testHttpResponseHasJsonContenTypeWhenReturnedByCustomExceptionHandler() {
        ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
        String message = "Forced error";
        when(warningService.getStatus(any())).thenThrow(new RuntimeException(message));

        ResponseEntity<String> response = restTemplate.postForEntity(statusUrl, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        System.out.println(response.getBody());
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }
    
	@Test
	public void testApiLogsErrorMessageWhenExceptionRaised() {
		ExposureStatusRequestVo request = new ExposureStatusRequestVo(new ArrayList<VisitTokenVo>());
		String message = "Forced error";
		when(warningService.getStatus(any())).thenThrow(new RuntimeException(message));

		ResponseEntity<String> response = restTemplate.postForEntity(statusUrl, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		List<ILoggingEvent> logsList = listAppender.list;
		assertThat(logsList.get(0).getMessage()).isEqualTo(message + ", requested uri: uri=" + statusUrl);
		assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
	}

	@Test
	public void testApiLogsBadRequestReasonWhenBadRequestResponse() {
		ExposureStatusRequestVo request = new ExposureStatusRequestVo(null);

		ResponseEntity<String> response = restTemplate.postForEntity(statusUrl, request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		List<ILoggingEvent> logsList = listAppender.list;
		assertThat(logsList.get(0).getMessage()).contains(
				"Invalid input data",
				"visitTokens",
				"nul",
				", requested uri: uri=" + statusUrl);
		assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
	}


	@Test
	public void testApiLogTacWarningUnauthorizedWhenFailedAuthorization() {	
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth("invalid JWT token");
		headers.setContentType(MediaType.APPLICATION_JSON);
		ReportRequestVo request = new ReportRequestVo(new ArrayList<VisitVo>());
		HttpEntity<ReportRequestVo> reportEntity = new HttpEntity<>(request, headers);

		String reportUrl = pathPrefixV2 + UriConstants.REPORT;
		ResponseEntity<String> response = restTemplate.postForEntity(reportUrl, reportEntity,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		List<ILoggingEvent> logsList = listAppender.list;
		assertThat(logsList.get(0).getMessage())
			.contains("TacWarningUnauthorizedException")
			.contains("requested uri: uri=" + reportUrl);
		assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
	}
}