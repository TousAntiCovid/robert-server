package e2e.robert.ws.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@Data
@SuperBuilder
public class RegisterVo {

    private String captchaId;

    private String captcha;

    private String clientPublicECDHKey;

    private PushInfoVo pushInfo;

}
