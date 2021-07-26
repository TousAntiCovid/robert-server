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
     * Request a captcha challenge ID.
     * Call /api/v5/captcha
     * @return String a captcha challenge identifier
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
     * Resuest a challenge image corresponding to the challenge identifier captchaId.
     * Call /api/v5/captcha/<<captchaId>>/image
     * @param captchaId Identifier of the challenge to retrieve
     * @return Response The captcha image to solve
     */
    public Response requestCaptchaChallengeById(String captchaId) {
        final String captchaByIdUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/captcha/").concat(captchaId).concat("/image");
        return this.apiGetCall(captchaByIdUrl);
    }

    /**
     * Register an application.
     * Call /api/register
     * @param user User
     * @return Response Result of the save operation
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
     * Reports a contact between the application and a list of other applications.
     * Call /api/report
     * @param appMobile Object simulating an application
     * @return Response Result of the reporting operation
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

    /**
     * Request an update of the risk status.
     * Call /api/status
     * @param exposureStatusRequest
     * @return Response
     */
    public Response requestStatus(ExposureStatusRequest exposureStatusRequest) {
        final String statusUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/status");

        Response res = given()
                .contentType(ContentType.JSON)
                .body(exposureStatusRequest)
                .when()
                .post(statusUrl);

        return res;
    }

    /**
     * Request the deletion of the risk exposure history.
     * Call /api/deleteExposureHistory
     * @param authentifiedRequest
     * @return Response
     */
    public Response deleteExposureHistory(AuthentifiedRequest authentifiedRequest) {
        final String statusUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/deleteExposureHistory");

        Response res = given()
                .contentType(ContentType.JSON)
                .body(authentifiedRequest)
                .when()
                .post(statusUrl);

        return res;
    }

    /**
     * Request the deletion of the application registration.
     * Call /api/unregister
     * @param unregisterRequest
     * @return Response
     */
    public Response unregister(UnregisterRequest unregisterRequest) {
        final String statusUrl = this.applicationProperties.getWsRest()
                .getBaseUrl().toString().concat("/unregister");

        Response res = given()
                .contentType(ContentType.JSON)
                .body(unregisterRequest)
                .when()
                .post(statusUrl);

        return res;
    }


}
