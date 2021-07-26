package fr.gouv.stopc.robert.integrationtest.feature;

import fr.gouv.stopc.robert.integrationtest.config.ApplicationProperties;
import fr.gouv.stopc.robert.integrationtest.enums.DigestSaltEnum;
import fr.gouv.stopc.robert.integrationtest.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.integrationtest.feature.context.ScenarioContext;
import fr.gouv.stopc.robert.integrationtest.feature.context.User;
import fr.gouv.stopc.robert.integrationtest.model.AppMobile;
import fr.gouv.stopc.robert.integrationtest.model.api.request.AuthentifiedRequest;
import fr.gouv.stopc.robert.integrationtest.model.api.request.ExposureStatusRequest;
import fr.gouv.stopc.robert.integrationtest.model.api.request.RegisterSuccessResponse;
import fr.gouv.stopc.robert.integrationtest.model.api.request.UnregisterRequest;
import fr.gouv.stopc.robert.integrationtest.model.api.response.ExposureStatusResponse;
import fr.gouv.stopc.robert.integrationtest.model.api.response.ReportResponse;
import fr.gouv.stopc.robert.integrationtest.model.api.response.UnregisterResponse;
import fr.gouv.stopc.robert.integrationtest.service.RobertWsService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.Assert.assertEquals;

@Slf4j
public class RobertClientStepDefinitions {

    public static final String CAPTCHA_BYPASS_SOLUTION = "IEDX";

    private final RobertWsService robertWsService;
    private final ScenarioContext scenarioContext;
    private final ApplicationProperties applicationProperties;
    private final Map<String, AppMobile> appMobileMap;

    public RobertClientStepDefinitions(final RobertWsService robertWsService,
                                       final ScenarioContext scenarioContext,
                                       final ApplicationProperties applicationProperties) {
        this.robertWsService = robertWsService;
        this.scenarioContext = scenarioContext;
        this.applicationProperties = applicationProperties;
        this.appMobileMap = new HashMap<>();
    }

    @Given("{string} has the application TAC")
    public void generate_user(String strpName) {
        this.scenarioContext.getOrCreateUser(strpName);
    }

    @Given("{string} registered on TAC with the Captcha service")
    public void request_and_resolve_captcha_challenge(String strpName) {

        // Request captchaId challenge
        Response resCaptchaChallId = this.robertWsService.requestCaptchaChallengeId();
        assertEquals(200, resCaptchaChallId.statusCode());
        String captchaId = resCaptchaChallId.getBody().jsonPath().get("captchaId");
        User user = this.scenarioContext.getUser(strpName);

        // On génère un faux id Captcha qui servira à différencier les appMobiles
        user.setCaptchaId(RandomStringUtils.random(7, true, false));// captchaId

        // Request a challenge by id
        Response resChallenge = this.robertWsService.requestCaptchaChallengeById(captchaId);// user.getCaptchaId()
        assertEquals(200, resChallenge.statusCode());

        // Simulate visual resolution
        user.setCaptchaSolution(this.CAPTCHA_BYPASS_SOLUTION);
        this.scenarioContext.updateUser(strpName, user);
    }

    @Then("{string} is registed on TAC")
    public void  register_user_on_TAC(String userName) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        User user = this.scenarioContext.getUser(userName);

        AppMobile appMobile = new AppMobile(user.getCaptchaId(),
                user.getCaptchaSolution(),
                this.applicationProperties.getCryptoPublicKey());
        user.setClientPublicECDHKey(appMobile.getPublicKey());
        RegisterSuccessResponse registerSuccessResponse = this.robertWsService.register(user);
        appMobile.decryptRegisterResponse(registerSuccessResponse);
        appMobileMap.put(user.getCaptchaId(), appMobile);
        this.scenarioContext.updateUser(userName, user);
    }

    @Given("{string} is near {string} during 1 hour")
    public void generate_contact_between_two_users(String firstUserName, String secondUserName) {
        User firstUser = this.scenarioContext.getUser(firstUserName);
        Objects.requireNonNull(firstUser);
        User secondUser = this.scenarioContext.getUser(secondUserName);
        Objects.requireNonNull(secondUser);

        List<String> limitedAppMobileIds = new ArrayList<>();
        limitedAppMobileIds.add(secondUser.getCaptchaId());

        AppMobile appMobile = appMobileMap.get(firstUser.getCaptchaId());
        Objects.requireNonNull(appMobile);

        appMobile.generateHelloMessageDuring(appMobile, limitedAppMobileIds, appMobileMap, 1);
    }

    @Given("{string} declare himself sick with a long code")
    public void report_contacts(String userName) {
        User user = this.scenarioContext.getUser(userName);
        Objects.requireNonNull(user);

        AppMobile appMobile = appMobileMap.get(user.getCaptchaId());
        Objects.requireNonNull(appMobile);

        Response resReport = this.robertWsService.reportContactHistory(appMobile);
        assertEquals(200, resReport.statusCode());
        ReportResponse repResponse = resReport.as(ReportResponse.class);
        assertEquals(true, repResponse.getSuccess());
        assertEquals("Successful operation", repResponse.getMessage());
    }

    @Given("{string} asks for exposure status")
    public void request_status_information(String userName) {
        User user = this.scenarioContext.getUser(userName);
        Objects.requireNonNull(user);

        AppMobile appMobile = appMobileMap.get(user.getCaptchaId());
        Objects.requireNonNull(appMobile);

        AuthentifiedRequest authentifiedRequest = appMobile.prepareAuthRequest(0, DigestSaltEnum.STATUS);

        ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
        exposureStatusRequest.setEbid(authentifiedRequest.getEbid());
        exposureStatusRequest.setEpochId(authentifiedRequest.getEpochId());
        exposureStatusRequest.setTime(authentifiedRequest.getTime());
        exposureStatusRequest.setMac(authentifiedRequest.getMac());

        // TODO : WS_Call
        Response resStatus = this.robertWsService.requestStatus(exposureStatusRequest);
//        throw new io.cucumber.java.PendingException("TODO: implement me");

//        A décommenter une fois l'appel fait
        ExposureStatusResponse exposureStatusResponse = resStatus.as(ExposureStatusResponse.class);
        // Est-ce que l'on stocke dans l'appmobile (riskLevel, lastContactDate, lastRiskScoringDate)
        user.setLastExposureStatusResponse(exposureStatusResponse);
        appMobile.decryptStatusResponse(exposureStatusResponse);
    }

    @Then("{string} is notified at risk")
    public void  is_user_at_risk(String userName) {
        User user = this.scenarioContext.getUser(userName);
        Objects.requireNonNull(user);

        // cf : PDF documentation (ROBERT-specification-EN-v1_1.pdf) Not at risk 0, at risk 1
        // TODO : à changer une fois le batch lancé (mettre 1)
        assertEquals(Integer.valueOf(0), user.getLastExposureStatusResponse().getRiskLevel());
    }

    @Given("{string} asks to delete her history")
    public void request_delete_exposure_history(String userName) {
        User user = this.scenarioContext.getUser(userName);
        Objects.requireNonNull(user);

        AppMobile appMobile = appMobileMap.get(user.getCaptchaId());
        Objects.requireNonNull(appMobile);

        AuthentifiedRequest authentifiedRequest = appMobile.prepareAuthRequest(0, DigestSaltEnum.DELETE_HISTORY);
        Response res = this.robertWsService.deleteExposureHistory(authentifiedRequest);
        UnregisterResponse unregisterResponse = res.as(UnregisterResponse.class);
        assertEquals(Boolean.TRUE, unregisterResponse.getSuccess());
    }

    @When("{string} ask to be unsubscribed from TAC")
    public void request_unregister(String userName) {
        User user = this.scenarioContext.getUser(userName);
        Objects.requireNonNull(user);

        AppMobile appMobile = appMobileMap.get(user.getCaptchaId());
        Objects.requireNonNull(appMobile);

        AuthentifiedRequest authentifiedRequest = appMobile.prepareAuthRequest(0, DigestSaltEnum.UNREGISTER);

        UnregisterRequest unregisterRequest = new UnregisterRequest();
        unregisterRequest.setEbid(authentifiedRequest.getEbid());
        unregisterRequest.setEpochId(authentifiedRequest.getEpochId());
        unregisterRequest.setTime(authentifiedRequest.getTime());
        unregisterRequest.setMac(authentifiedRequest.getMac());

        Response res = this.robertWsService.unregister(unregisterRequest);
        UnregisterResponse unregisterResponse = res.as(UnregisterResponse.class);
        assertEquals(Boolean.TRUE, unregisterResponse.getSuccess());
    }

}
