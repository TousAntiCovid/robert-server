package fr.gouv.stopc.robert.integrationtest.model.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CaptchaCreationVo {

    @NotNull
    private String type;

    @NotNull
    private String locale;
}
