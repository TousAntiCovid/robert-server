package fr.gouv.tacw.ws.vo;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExposureStatusRequestVo {
	@NotNull
	@Valid
	private List<VisitTokenVo> visitTokens;
}
