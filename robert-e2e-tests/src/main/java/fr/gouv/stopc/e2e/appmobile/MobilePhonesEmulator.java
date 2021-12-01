package fr.gouv.stopc.e2e.appmobile;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class MobilePhonesEmulator {

    @Getter
    final Map<String, AppMobile> applicationMobileMap = new HashMap<>();

    public void exchangeHelloMessagesWith(final List<AppMobile> appMobileList,
            final Instant startInstant,
            final Duration exchangeDuration) {
        final var endDate = startInstant.plus(exchangeDuration);

        for (AppMobile appMobile : appMobileList) {

            // For each app, we generate helloMessages
            Stream.iterate(startInstant, d -> d.isBefore(endDate), d -> d.plusSeconds(10))
                    .map(appMobile::produceHelloMessage)
                    .forEach(
                            hello -> appMobileList.stream()
                                    .filter(app -> app != appMobile)
                                    .forEach(appMobile1 -> appMobile1.receiveHelloMessage(hello))
                    );
        }
    }
}
