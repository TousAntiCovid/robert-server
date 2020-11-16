package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.gouv.tacw.ws.dto.ExposureStatusResponseDto;
import fr.gouv.tacw.ws.service.WarningService;
import fr.gouv.tacw.ws.utils.UriConstants;
import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;
import fr.gouv.tacw.ws.vo.VisitVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@MockBean(WarningService.class)
class TacWarningWsRestApplicationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private WarningService warningService;
	
	@Value("${controller.path.prefix}" + UriConstants.API_V1)
	private String pathPrefixV1;

	@Test
	void testInfectedUserCanReportItselfAsInfected() {
		ReportRequestVo request = new ReportRequestVo(new ArrayList<VisitVo>());
		ResponseEntity<String> response = restTemplate.postForEntity(pathPrefixV1 + UriConstants.REPORT, request,
				String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void testCanGetStatus() {
		ExposureStatusRequestVo request = new ExposureStatusRequestVo( new ArrayList<VisitTokenVo>() );
		when(warningService.getStatus(any(ExposureStatusRequestVo.class))).thenReturn(true);

		ResponseEntity<ExposureStatusResponseDto> response = restTemplate.postForEntity(pathPrefixV1 + UriConstants.STATUS, request,
				ExposureStatusResponseDto.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}