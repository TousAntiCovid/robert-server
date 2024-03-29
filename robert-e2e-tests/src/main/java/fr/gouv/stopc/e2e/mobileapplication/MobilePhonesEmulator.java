package fr.gouv.stopc.e2e.mobileapplication;

import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.mobileapplication.timemachine.repository.ClientIdentifierRepository;
import fr.gouv.stopc.e2e.mobileapplication.timemachine.repository.RegistrationRepository;
import fr.gouv.stopc.e2e.steps.PlatformTimeSteps;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.RobertLegacyApi;
import io.cucumber.spring.ScenarioScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
@ScenarioScope
@RequiredArgsConstructor
public class MobilePhonesEmulator {

    private final ApplicationProperties applicationProperties;

    private final RobertLegacyApi robertLegacyApi;

    private final CaptchaApi captchaApi;

    private final ClientIdentifierRepository clientIdentifierRepository;

    private final RegistrationRepository registrationRepository;

    private final Map<String, MobileApplication> mobileApplications = new HashMap<>();

    private final PlatformTimeSteps platformTimeSteps;

    public MobileApplication getMobileApplication(final String userName) {
        return mobileApplications.get(userName);
    }

    public void createMobileApplication(final String userName) {
        final var mobileApplication = new MobileApplication(
                userName,
                applicationProperties,
                captchaApi,
                robertLegacyApi,
                clientIdentifierRepository,
                registrationRepository,
                platformTimeSteps
        );
        mobileApplications.put(userName, mobileApplication);
    }

    public void exchangeHelloMessagesBetween(final List<String> users,
            final Instant startInstant,
            final Duration exchangeDuration) {
        final var endDate = startInstant.plus(exchangeDuration);

        for (final String user : users) {
            final var mobileApplication = mobileApplications.get(user);
            Stream.iterate(startInstant, d -> d.isBefore(endDate), d -> d.plusSeconds(1))
                    .map(mobileApplication::produceHelloMessage)
                    .forEach(
                            hello -> users.stream()
                                    .filter(otherUser -> !otherUser.equals(user))
                                    .map(mobileApplications::get)
                                    .forEach(mobileApp -> mobileApp.receiveHelloMessage(hello))
                    );
        }
    }
}
