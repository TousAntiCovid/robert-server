package fr.gouv.stopc.robert.integrationtest.model.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CaptchaResultVo {

    @NotNull
    private String id;

    @NotNull
    private String captchaId;
}
