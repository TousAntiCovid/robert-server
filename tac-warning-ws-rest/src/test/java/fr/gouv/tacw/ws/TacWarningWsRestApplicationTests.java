package fr.gouv.tacw.ws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.gouv.tacw.ws.dto.StatusResponseDto;
import fr.gouv.tacw.ws.vo.ReportRequestVo;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TacWarningWsRestApplicationTests {
	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testInfectedUserCanDeclareItselfAsInfected() {
		ReportRequestVo request = ReportRequestVo.builder().build();
		
		ResponseEntity<StatusResponseDto> response = restTemplate.postForEntity("/api/v1/TACW_REPORT", request, StatusResponseDto.class);
		
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().getReturnCode()).isEqualTo(StatusResponseDto.code.OK);
	}

}