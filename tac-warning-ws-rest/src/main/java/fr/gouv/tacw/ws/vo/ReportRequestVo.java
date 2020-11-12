package fr.gouv.tacw.ws.vo;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportRequestVo {
	private List<QRcodeVo> localList;
}
