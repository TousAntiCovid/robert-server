package fr.gouv.stopc.robert.crypto.grpc.server.contributors;

import fr.gouv.stopc.robertserver.common.RobertClock;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RobertClockInfoContributor implements InfoContributor {

    private final RobertClock clock;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail(
                "robertClock", Map.of(
                        "currentTime", clock.now().asInstant().toString(),
                        "currentEpoch", clock.now().asEpochId()
                )
        );
    }
}
