package fr.gouv.tac.e2e.robert.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class Register {

    private String captchaId;

    private String captcha;

    private String clientPublicECDHKey;

    private PushInfo pushInfo;

}
