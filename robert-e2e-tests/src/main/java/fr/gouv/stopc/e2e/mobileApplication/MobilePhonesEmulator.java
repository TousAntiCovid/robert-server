package fr.gouv.stopc.e2e.mobileApplication;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.DefaultApi;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class MobilePhonesEmulator {

    private final ApplicationProperties applicationProperties;

    private final DefaultApi robertApi;

    private final CaptchaApi captchaApi;

    final Map<String, MobileApplication> applicationMobileMap = new HashMap<>();

    public MobileApplication getMobileApplication(final String userName) {
        return applicationMobileMap.get(userName);
    }

    private List<MobileApplication> getMobileApplicationList(final List<String> users) {
        return users.stream()
                .filter(applicationMobileMap::containsKey)
                .map(applicationMobileMap::get)
                .collect(Collectors.toList());
    }

    public void createMobileApplication(final String userName) {
        final var mobileApplication = new MobileApplication(userName, applicationProperties, captchaApi, robertApi);
        applicationMobileMap.put(userName, mobileApplication);
    }

    public void exchangeHelloMessagesBetween(final List<String> users,
            final Instant startInstant,
            final Duration exchangeDuration) {
        final var endDate = startInstant.plus(exchangeDuration);

        final var mobileApplications = getMobileApplicationList(users);

        for (MobileApplication mobileApplication : mobileApplications) {

            // For each app, we generate helloMessages
            Stream.iterate(startInstant, d -> d.isBefore(endDate), d -> d.plusSeconds(10))
                    .map(mobileApplication::produceHelloMessage)
                    .forEach(
                            hello -> mobileApplications.stream()
                                    .filter(app -> app != mobileApplication)
                                    .forEach(mobileApp -> mobileApp.receiveHelloMessage(hello))
                    );
        }
    }
}
