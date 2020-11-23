package fr.gouv.tacw.ws.vo;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VisitTokenVo {
	@NotNull
	@Valid
	private TokenTypeVo type;
	
	@NotNull
    @Size(max = 500) // TODO compute the maximum size of a payload
	private String payload;
}
