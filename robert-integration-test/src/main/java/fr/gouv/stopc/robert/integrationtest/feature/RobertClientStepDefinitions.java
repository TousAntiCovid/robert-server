package fr.gouv.stopc.robert.integrationtest.feature;

import fr.gouv.stopc.robert.integrationtest.config.ApplicationProperties;
import fr.gouv.stopc.robert.integrationtest.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.integrationtest.feature.context.ScenarioContext;
import fr.gouv.stopc.robert.integrationtest.feature.context.User;
import fr.gouv.stopc.robert.integrationtest.model.AppMobile;
import fr.gouv.stopc.robert.integrationtest.model.api.request.RegisterSuccessResponse;
import fr.gouv.stopc.robert.integrationtest.model.api.response.ReportResponse;
import fr.gouv.stopc.robert.integrationtest.service.RobertWsService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
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

    @Given("{string} registred on TAC with the Captcha sevice")
    public void request_and_resolve_captcha_challenge(String strpName) {

        // Request captchaId challenge
        Response resCaptchaChallId = this.robertWsService.requestCaptchaChallengeId();
        assertEquals(200, resCaptchaChallId.statusCode());
        String captchaId = resCaptchaChallId.getBody().jsonPath().get("captchaId");
        User user = this.scenarioContext.getUser(strpName);

        // On génère un faux id Captcha qui servira à différencier les appMobiles
        user.setCaptchaId(RandomStringUtils.random(7, true, false));// captchaId

        // Request a challenge by id (optional cf : robert.server.disable-check-captcha & token)
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
        throw new io.cucumber.java.PendingException("TODO: implement me");
    }

    @Given("{string} asks to delete her history")
    public void request_delete_exposure_history(String userName) {
        throw new io.cucumber.java.PendingException("TODO: implement me");
    }

    @Given("{string}  asks for unregisted to TAC")
    public void request_unregister(String userName) {
        throw new io.cucumber.java.PendingException("TODO: implement me");
    }

}
