package fr.gouv.tacw.ws.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class PropertyLoader {

    @Value("${robert.jwt.publickey}")
    private String jwtPublicKey;

    @Value("${tacw.jwt.report.authorization.disabled}")
    private Boolean jwtReportAuthorizationDisabled;

}
