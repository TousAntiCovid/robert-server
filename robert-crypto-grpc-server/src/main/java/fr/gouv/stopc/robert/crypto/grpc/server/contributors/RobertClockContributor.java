package fr.gouv.stopc.robert.crypto.grpc.server.contributors;

import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

import static java.lang.String.valueOf;
import static java.time.Instant.*;

@Component
@RequiredArgsConstructor
public class RobertClockContributor implements InfoContributor {

    private final IServerConfigurationService serverConfigurationService;

    @Override
    public void contribute(Info.Builder builder) {

        final var clock = new RobertClock(serverConfigurationService.getServiceTimeStart());
        var robertClockDetails = Map.of(
                "startTime", valueOf(clock.atNtpTimestamp(serverConfigurationService.getServiceTimeStart())),
                "startNtpTimestamp",
                valueOf(clock.atNtpTimestamp(serverConfigurationService.getServiceTimeStart()).asNtpTimestamp()),
                "currentTime", valueOf(clock.at(now())),
                "currentEpoch", valueOf(clock.at(now()).asEpochId())
        );
        builder.withDetail("robert-clock", robertClockDetails);
    }
}
