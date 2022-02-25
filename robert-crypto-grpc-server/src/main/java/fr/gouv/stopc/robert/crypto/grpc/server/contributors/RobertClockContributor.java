package fr.gouv.stopc.robert.crypto.grpc.server.contributors;

import fr.gouv.stopc.robert.server.common.service.IServerConfigurationService;
import fr.gouv.stopc.robert.server.common.service.RobertClock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RobertClockContributor implements InfoContributor {

    private final IServerConfigurationService serverConfigurationService;

    @Override
    public void contribute(Info.Builder builder) {
        RobertClock clock = new RobertClock(serverConfigurationService.getServiceTimeStart());
        Map<String, String> robertClockDetails = new HashMap<>();
        robertClockDetails.put(
                "startTime", String.valueOf(clock.atNtpTimestamp(serverConfigurationService.getServiceTimeStart()))
        );
        robertClockDetails.put(
                "startNtpTimestamp",
                String.valueOf(clock.atNtpTimestamp(serverConfigurationService.getServiceTimeStart()).asNtpTimestamp())
        );
        robertClockDetails.put("currentTime", String.valueOf(clock.at(Instant.now())));
        robertClockDetails.put("currentEpoch", String.valueOf(clock.at(Instant.now()).asEpochId()));

        builder.withDetail("robert-clock", robertClockDetails);
    }
}
