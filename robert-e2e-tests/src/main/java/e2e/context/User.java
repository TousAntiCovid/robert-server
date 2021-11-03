package e2e.context;

import e2e.phone.AppMobile;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class User {

    private final String name;

    private String captchaId;

    private String captchaSolution;

    private String clientPublicECDHKey;

    private AppMobile appMobile;
}
