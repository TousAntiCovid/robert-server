package fr.gouv.stopc.robertserver.ws.config;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/*
 * Global configuration of the Robert Server WS application, which is editable through Consul.
 */
@Getter
@ToString
@Component
@RefreshScope
public class WsServerConfiguration {

    @Value("${robert.epoch-bundle-duration-in-days}")
    private Integer epochBundleDurationInDays;

    @Value("${robert.server.status-request-minimum-epoch-gap}")
    private Integer statusRequestMinimumEpochGap;

    @Value("${robert.jwt.privatekey}")
    private String jwtPrivateKey;

    @Value("${robert.jwt.lifetime}")
    private int jwtLifeTime;

    @Value("${robert.jwt.use-transient-key}")
    private Boolean jwtUseTransientKey;

    @Value("${robert.jwt.declare.public-kid}")
    private String declareTokenKid;

    @Value("${robert.jwt.declare.private-key}")
    private String declareTokenPrivateKey;
}
