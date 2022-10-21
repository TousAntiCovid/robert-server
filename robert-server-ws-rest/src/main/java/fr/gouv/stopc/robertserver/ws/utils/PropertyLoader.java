package fr.gouv.stopc.robertserver.ws.utils;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Getter
@Component
public class PropertyLoader {

    @Value("${robert.crypto.server.host}")
    private String cryptoServerHost;

    @Value("${robert.crypto.server.port}")
    private String cryptoServerPort;

    @Value("${submission.code.server.url}")
    private URI serverCodeUrl;

    @Value("${robert.esr.limit}")
    private Integer esrLimit;

    @Value("${robert.server.request-time-delta-tolerance}")
    private Integer requestTimeDeltaTolerance;

    @Value("${controller.internal.path.prefix}")
    private String internalPathPrefix;

    @Value("${push.server.host}")
    private String pushServerHost;

    @Value("${push.server.port}")
    private String pushServerPort;

    @Value("${push.api.version}")
    private String pushApiVersion;

    @Value("${push.api.path}")
    private String pushApiPath;

    @Value("${push.api.path.token}")
    private String pushApiTokenPath;

    @Value("${robert.jwt.privatekey}")
    private String jwtPrivateKey;

    @Value("${robert.jwt.lifetime}")
    private int jwtLifeTime;

    @Value("${robert.server.disable-check-token}")
    private Boolean disableCheckToken;
}
