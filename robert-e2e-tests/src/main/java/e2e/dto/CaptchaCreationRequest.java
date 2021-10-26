package e2e.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CaptchaCreationRequest {

    String type;

    String locale;
}
