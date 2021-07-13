package fr.gouv.stopc.robert.integrationtest.service;

import fr.gouv.stopc.robert.integrationtest.config.ApplicationProperties;
import fr.gouv.stopc.robert.integrationtest.feature.context.ScenarioContext;
import fr.gouv.stopc.robert.integrationtest.feature.context.User;
import fr.gouv.stopc.robert.integrationtest.model.api.request.CaptchaCreationVo;
import fr.gouv.stopc.robert.integrationtest.model.api.request.PushInfoVo;
import fr.gouv.stopc.robert.integrationtest.model.api.request.RegisterSuccessResponse;
import fr.gouv.stopc.robert.integrationtest.model.api.request.RegisterVo;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static io.restassured.RestAssured.given;

@Service
public class RobertWsService {

    private final ApplicationProperties applicationProperties;

    private final ScenarioContext scenarioContext;

    public RobertWsService(final ApplicationProperties applicationProperties,
                           final ScenarioContext scenarioContext) {
        this.applicationProperties = applicationProperties;
        this.scenarioContext = Objects.requireNonNull(scenarioContext, "scenarioAppContext must not be null");
    }

    private Response apiPostCall(Object body, String url) {
        Response res = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(url);
        return res;
    }

    private Response apiGetCall(String url) {
        Response res = given()
                .when()
                .get(url);
        return res;
    }

    /**
     * Demande un identifiant de challenge captcha
     * @return String identifiant de challenge captcha
     */
    public Response requestCaptchaChallengeId() {
        // Payload
        CaptchaCreationVo captcha = new CaptchaCreationVo();
        captcha.setLocale("fr");
        captcha.setType("IMAGE");
        // Url
        final String captchaUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/captcha");
        // Call
        return this.apiPostCall(captcha, captchaUrl);
    }

    /**
     * Appel l'url /api/v5/captcha/<<captchaId>>/image
     * @return L'image du captcha à résoudre
     */
    public Response requestCaptchaChallengeById(String captchaId) {
        // Url
        final String captchaByIdUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/captcha/").concat(captchaId).concat("/image");
        return this.apiGetCall(captchaByIdUrl);
    }

    /**
     *
     * @param user Utilisateur
     * @return Response
     */
    public RegisterSuccessResponse register(User user) {
        RegisterVo registerVo = new RegisterVo();
        registerVo.setCaptchaId(user.getCaptchaId());
        registerVo.setCaptcha(user.getCaptchaSolution());
        registerVo.setClientPublicECDHKey(user.getClientPublicECDHKey());

        PushInfoVo pushInfo = new PushInfoVo();
        pushInfo.setToken("string");
        pushInfo.setLocale("fr");
        pushInfo.setTimezone("Europe/Paris");

        registerVo.setPushInfo(pushInfo);

        final String registerUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/register");

        RegisterSuccessResponse res = given()
                .contentType(ContentType.JSON)
                .body(registerVo)
                .when()
                .post(registerUrl).as(RegisterSuccessResponse.class);
        return res;
    }

    public Response reportContactHistory() {
        // TODO
        return null;
    }

}
