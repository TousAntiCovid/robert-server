package fr.gouv.stopc.e2e.mobileapplication;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.mobileapplication.repository.ApplicationIdentityRepository;
import fr.gouv.stopc.e2e.mobileapplication.repository.CaptchaRepository;
import fr.gouv.stopc.e2e.mobileapplication.repository.RegistrationRepository;
import fr.gouv.stopc.e2e.steps.PlatformTimeSteps;
import fr.gouv.stopc.robert.client.api.CaptchaApi;
import fr.gouv.stopc.robert.client.api.RobertApi;
import io.cucumber.spring.ScenarioScope;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static com.nimbusds.jose.JOSEObjectType.JWT;
import static com.nimbusds.jose.JWSAlgorithm.ES256;
import static com.nimbusds.jose.jwk.Curve.P_256;

@Service
@ScenarioScope
@RequiredArgsConstructor
public class MobilePhonesEmulator {

    private final ApplicationProperties applicationProperties;

    private final RobertApi robertApi;

    private final CaptchaApi captchaApi;

    private final CaptchaRepository captchaRepository;

    private final ApplicationIdentityRepository applicationIdentityRepository;

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
                robertApi,
                captchaRepository,
                applicationIdentityRepository,
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

    @SneakyThrows
    public String generateReportCode() {
        final var key = Base64.getDecoder().decode(applicationProperties.getSubmissionJwtSigningKey());
        final var privateKey = KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(key));
        final var jwt = new SignedJWT(
                new JWSHeader.Builder(ES256)
                        .type(JWT)
                        .keyID("E2E_TESTS_KEY")
                        .build(),
                new JWTClaimsSet.Builder()
                        .issuer("SIDEP")
                        .claim("iat", new Date())
                        .claim("jti", UUID.randomUUID())
                        .build()
        );
        jwt.sign(new ECDSASigner(privateKey, P_256));
        return jwt.serialize();
    }
}
