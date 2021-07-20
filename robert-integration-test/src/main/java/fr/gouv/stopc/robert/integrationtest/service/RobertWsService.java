package fr.gouv.stopc.robert.integrationtest.service;

import fr.gouv.stopc.robert.integrationtest.config.ApplicationProperties;
import fr.gouv.stopc.robert.integrationtest.feature.context.ScenarioContext;
import fr.gouv.stopc.robert.integrationtest.feature.context.User;
import fr.gouv.stopc.robert.integrationtest.model.AppMobile;
import fr.gouv.stopc.robert.integrationtest.model.api.request.*;
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
     * Demande un identifiant de challenge captcha.
     * Appel l'url /api/v5/captcha
     * @return String un identifiant de challenge captcha
     */
    public Response requestCaptchaChallengeId() {
        // Payload
        CaptchaCreationVo captcha = new CaptchaCreationVo();
        captcha.setLocale("fr");
        captcha.setType("IMAGE");

        final String captchaUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/captcha");

        return this.apiPostCall(captcha, captchaUrl);
    }

    /**
     * Demande une image de challenge à résoudre correspondant à l'identifiant de challenge captchaId.
     * Appel l'url /api/v5/captcha/<<captchaId>>/image
     * @param captchaId Identifiant du challenge à récupérer
     * @return Response L'image du captcha à résoudre
     */
    public Response requestCaptchaChallengeById(String captchaId) {
        final String captchaByIdUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/captcha/").concat(captchaId).concat("/image");
        return this.apiGetCall(captchaByIdUrl);
    }

    /**
     * Enregistre une application
     * Appel l'url /api/register
     * @param user Utilisateur
     * @return Response Résultat de l'opération d'enregistrement
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

    /**
     * Reporte un contact entre l'application et une liste d'autres applications
     * Appel l'url /api/report
     * @param appMobile Objet simulant une application
     * @return Response Résultat de l'opération de reporting
     */
    public Response reportContactHistory(AppMobile appMobile) {
        final String reportUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/report");

        ReportRequest reportReq = new ReportRequest();
        // QrCode : le service de vérfication du token est mocké en dev et bypassé en intégration
        reportReq.setToken("AAAAAA");
        reportReq.setContacts(appMobile.getContactsAndRemoveThem());

        Response res = given()
                .contentType(ContentType.JSON)
                .body(reportReq)
                .when()
                .post(reportUrl);

        return res;
    }

}
