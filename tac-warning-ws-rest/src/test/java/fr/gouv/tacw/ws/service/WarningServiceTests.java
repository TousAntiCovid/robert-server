package fr.gouv.tacw.ws.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.VisitTokenVo;

@SpringBootTest(classes = { WarningService.class })
public class WarningServiceTests {
	@Autowired
	private WarningService warningService;

	@Test
	public void testStatusOfVisitTokenNotInfectedIsNotAtRisk() {
		List<VisitTokenVo> visitTokens = new ArrayList<VisitTokenVo>();
		visitTokens.add(new VisitTokenVo(VisitTokenVo.tokenType.STATIC,
				"0YWN3LXR5cGUiOiJTVEFUSUMiLCJ0YWN3LXZlcnNpb24iOjEsImVyc"));

		ExposureStatusRequestVo statusRequestVo = new ExposureStatusRequestVo(visitTokens);

		assertThat(warningService.getStatus(statusRequestVo)).isFalse();
	}
}
