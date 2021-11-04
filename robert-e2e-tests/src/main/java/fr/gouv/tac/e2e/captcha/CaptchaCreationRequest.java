package fr.gouv.tac.e2e.captcha;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CaptchaCreationRequest {

    String type;

    String locale;
}
