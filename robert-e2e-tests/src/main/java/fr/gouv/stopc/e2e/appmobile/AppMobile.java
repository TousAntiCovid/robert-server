package fr.gouv.stopc.e2e.appmobile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.stopc.e2e.appmobile.model.ClientIdentifierBundle;
import fr.gouv.stopc.e2e.config.ApplicationProperties;
import fr.gouv.stopc.e2e.external.crypto.CryptoAESGCM;
import fr.gouv.stopc.e2e.external.crypto.model.EphemeralTupleJson;
import fr.gouv.stopc.e2e.robert.model.CaptchaGenerationRequest;
import fr.gouv.stopc.e2e.robert.model.PushInfo;
import fr.gouv.stopc.e2e.robert.model.RegisterRequest;
import fr.gouv.stopc.e2e.robert.model.RegisterSuccessResponse;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import javax.validation.constraints.NotNull;

import java.security.KeyPair;
import java.util.Base64;
import java.util.List;

import static fr.gouv.stopc.e2e.appmobile.model.EcdhUtils.deriveKeysFromBackendPublicKey;
import static fr.gouv.stopc.e2e.appmobile.model.EcdhUtils.generateKeyPair;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;

@Slf4j
public class AppMobile {

    public static final String CAPTCHA_BYPASS_SOLUTION = "IEDX";

    private final ApplicationProperties applicationProperties;

    private final KeyPair keyPair;

    private String captchaId;

    private long timestart;

    private List<EphemeralTupleJson> decodedTuples;

    private ClientIdentifierBundle clientIdentifierBundleWithPublicKey;

    @SneakyThrows
    public AppMobile(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.keyPair = generateKeyPair();
        generateKeyForTuples();
        resolveCaptcha();
        register();
    }

    private RequestSpecification givenRobertBaseUri() {
        return given()
                .baseUri(applicationProperties.getWsRestBaseUrl())
                .contentType(JSON);
    }

    private void resolveCaptcha() {
        givenRobertBaseUri()
                .body(
                        CaptchaGenerationRequest.builder()
                                .locale("fr")
                                .type("IMAGE")
                                .build()
                )
                .when()
                .post("/api/v6/captcha")
                .then()
                .statusCode(200);

        // We generate a fake Captcha id which we will use to differentiate mobile apps
        captchaId = RandomStringUtils.random(7, true, false);

        // We simulate captcha resolution and we call the robert endpoint
        givenRobertBaseUri()
                .when()
                .get("/api/v6/captcha/{captchaId}/image", captchaId)
                .then()
                .statusCode(200);
    }

    private void register() {
        var register = RegisterRequest.builder()
                .captcha(CAPTCHA_BYPASS_SOLUTION)
                .captchaId(captchaId)
                .clientPublicECDHKey(getPublicKey())
                .pushInfo(
                        PushInfo.builder()
                                .token("string")
                                .locale("fr")
                                .timezone("Europe/Paris")
                                .build()
                )
                .build();

        var registerSuccessResponse = given()
                .contentType(JSON)
                .body(register)
                .when()
                .post(applicationProperties.getWsRestBaseUrl().concat("/api/v6/register"))
                .then()
                .statusCode(201)
                .extract()
                .as(RegisterSuccessResponse.class);

        decryptRegisterResponse(registerSuccessResponse);
    }

    @SneakyThrows
    private void generateKeyForTuples() {
        deriveKeysFromBackendPublicKey(
                Base64.getDecoder().decode(applicationProperties.getCryptoPublicKey()), keyPair
        )
                .ifPresent(clientIdentifierBundle -> this.clientIdentifierBundleWithPublicKey = clientIdentifierBundle);
    }

    private String getPublicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    private void decryptRegisterResponse(@NotNull RegisterSuccessResponse registerData) {
        this.timestart = registerData.getTimeStart();
        updateTuples(registerData.getTuples());
    }

    @SneakyThrows
    private void updateTuples(byte[] encryptedTuples) {
        var aesGcm = new CryptoAESGCM(clientIdentifierBundleWithPublicKey.getKeyForTuples());

        this.decodedTuples = new ObjectMapper().readValue(
                aesGcm.decrypt(encryptedTuples),
                new TypeReference<>() {
                }
        );

    }

}
