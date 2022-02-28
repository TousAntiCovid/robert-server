package fr.gouv.stopc.robert.crypto.grpc.server.contributors;

import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.time.Instant.*;

@Component
@RequiredArgsConstructor
public class RobertClockContributor implements InfoContributor {

    private final IServerConfigurationService serverConfigurationService;

    private final RobertClock clock;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail(
                "robert-clock", Map.of(
                        "startTime", clock.atNtpTimestamp(serverConfigurationService.getServiceTimeStart()).toString(),
                        "currentTime", clock.at(now()).toString(),
                        "currentEpoch", clock.at(now()).asEpochId()
                )
        );
    }
}
