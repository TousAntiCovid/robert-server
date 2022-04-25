package test.fr.gouv.stopc.robertserver.ws.service.impl;

import com.jayway.jsonpath.JsonPath;
import fr.gouv.stopc.robertserver.ws.config.WsServerConfiguration;
import fr.gouv.stopc.robertserver.ws.service.impl.DeclarationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static io.jsonwebtoken.Claims.EXPIRATION;
import static io.jsonwebtoken.Claims.ISSUED_AT;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

public class DeclarationServiceImplTest {

    private String base64EncodedPrivateKey;

    @BeforeEach
    public void generateKeys() throws NoSuchAlgorithmException {
        final var keys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        base64EncodedPrivateKey = Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded());
    }

    @Test
    public void should_generate_analytics_token_with_expired_date_rounded_to_minute() {
        // GIVEN
        final var wsServerConfiguration = new WsServerConfiguration();
        wsServerConfiguration.setAnalyticsTokenLifeTime(5L);
        wsServerConfiguration.setAnalyticsTokenPrivateKey(base64EncodedPrivateKey);

        DeclarationServiceImpl declarationService = new DeclarationServiceImpl(wsServerConfiguration);

        // WHEN
        var jwtToken = declarationService.generateAnalyticsToken().get();

        // THEN
        var splitToken = jwtToken.split("\\.");
        var payload = new String(Base64.getDecoder().decode(splitToken[1]));

        var issueAtInstant = extractInstantFromPayload(ISSUED_AT, payload);
        var expirationInstant = extractInstantFromPayload(EXPIRATION, payload);

        assertThat(issueAtInstant.atZone(UTC).getSecond()).isEqualTo(0);
        assertThat(issueAtInstant.atZone(UTC).getNano()).isEqualTo(0);

        assertThat(expirationInstant.atZone(UTC).getSecond()).isEqualTo(0);
        assertThat(expirationInstant.atZone(UTC).getNano()).isEqualTo(0);

        assertThat(Duration.between(issueAtInstant, expirationInstant)).isEqualTo(Duration.of(6, MINUTES));
    }

    private Instant extractInstantFromPayload(String key, String payload) {
        Integer timeStamp = JsonPath.compile(key).read(payload);
        return Instant.ofEpochSecond(timeStamp.longValue());
    }

}
