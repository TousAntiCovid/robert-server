package fr.gouv.tacw.ws.service;

import fr.gouv.tacw.ws.vo.ExposureStatusRequestVo;
import fr.gouv.tacw.ws.vo.ReportRequestVo;

public interface WarningService {
	public boolean getStatus(ExposureStatusRequestVo statusRequestVo);
	public void reportVisitsWhenInfected(ReportRequestVo reportRequestVo);
}
