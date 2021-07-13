package fr.gouv.stopc.robert.integrationtest.feature;

import fr.gouv.stopc.robert.integrationtest.config.ApplicationProperties;
import fr.gouv.stopc.robert.integrationtest.exception.RobertServerCryptoException;
import fr.gouv.stopc.robert.integrationtest.feature.context.ScenarioContext;
import fr.gouv.stopc.robert.integrationtest.feature.context.User;
import fr.gouv.stopc.robert.integrationtest.model.AppMobile;
import fr.gouv.stopc.robert.integrationtest.model.api.request.RegisterSuccessResponse;
import fr.gouv.stopc.robert.integrationtest.service.RobertWsService;
import fr.gouv.stopc.robert.integrationtest.service.TestService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.Assert.assertEquals;

@Slf4j
public class RobertClientStepDefinitions {
    /**
     * - Faire évoluer AppMobile pour ajouter les infos ex "user name", init avec uniquement le name et supprimer classe user
     * - Mettre appMobileMap dans context et non sur le StepDefinitinon
     * - Modifier TestService et appMobile cf ligne 103
     * - Créer des objets pour caster les responses du WsService et utiliser les assertions de restAssured
     * - Voir pour faire porter par TestService les étapes intermédiaires et non StepDefinitions
     */

    public static final String CAPTCHA_BYPASS_SOLUTION = "IEDX";

    private final RobertWsService robertWsService;
    private final TestService testService;
    private final ScenarioContext scenarioContext;
    private final ApplicationProperties applicationProperties;
    private final Map<String, AppMobile> appMobileMap;

    public RobertClientStepDefinitions(final RobertWsService robertWsService,
                                       final TestService testService,
                                       final ScenarioContext scenarioContext,
                                       final ApplicationProperties applicationProperties) {
        this.robertWsService = robertWsService;
        this.testService = testService;
        this.scenarioContext = scenarioContext;
        this.applicationProperties = applicationProperties;
        this.appMobileMap = new HashMap<>();
    }

    @Given("{string} has the application TAC")
    public void has_the_application_TAC(String strpName) {
        this.scenarioContext.getOrCreateUser(strpName);
    }

    @Given("{string} registred on TAC with the Captcha sevice")
    public void registred_on_TAC_with_the_Captcha_sevice(String strpName) {

        // Request captchaId challenge
        Response resCaptchaChallId = this.robertWsService.requestCaptchaChallengeId();
        assertEquals(200, resCaptchaChallId.statusCode());
        String captchaId = resCaptchaChallId.getBody().jsonPath().get("captchaId");
        User user = this.scenarioContext.getUser(strpName);
        user.setCaptchaId(captchaId);

        // Request a challenge by id (optional cf : robert.server.disable-check-captcha & token)
        Response resChallenge = this.robertWsService.requestCaptchaChallengeById(user.getCaptchaId());
        assertEquals(200, resChallenge.statusCode());

        // Simulate visual resolution
        user.setCaptchaSolution(this.CAPTCHA_BYPASS_SOLUTION);
        this.scenarioContext.updateUser(strpName, user);
    }

    @Then("{string} is registed on TAC")
    public void  is_registed_on_TAC(String strpName) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        User user = this.scenarioContext.getUser(strpName);

        // Create AppMobile
        AppMobile appMobile = new AppMobile(user.getCaptchaId(),
                user.getCaptchaSolution(),
                this.applicationProperties.getCryptoPublicKey());

        // TO CHECK : still used ?
        user.setClientPublicECDHKey(appMobile.getPublicKey());

        // Register
        RegisterSuccessResponse registerSuccessResponse = this.robertWsService.register(user);
        appMobile.decryptRegisterResponse(registerSuccessResponse);
        appMobileMap.put(user.getCaptchaId(), appMobile);
        this.scenarioContext.updateUser(strpName, user);
    }

    @Given("{string} is near {string} during 1 hour")
    public void report_contact_between_two_users(String firstUserName, String secondUserName) {
        User firstUser = this.scenarioContext.getUser(firstUserName);
        Objects.requireNonNull(firstUser);
        User secondUser = this.scenarioContext.getUser(secondUserName);
        Objects.requireNonNull(secondUser);

        List<String> limitedAppMobileIds = new ArrayList<>();
        limitedAppMobileIds.add(secondUser.getCaptchaId());

        // TODO :
        // Ici , on ne simulera pas des envois avec un timer
        // il faudra boucler pour générer directement les hello message sans utiliser
        // scheduleAtFixedRate. Donc itérer sur exchangeEbIdWith et simuler en prenant now
        // pour le premier puis itérer pour simuler une heure virtelle d'échanges par exemple
        AppMobile appMobile = appMobileMap.get(firstUser.getCaptchaId());
        Objects.requireNonNull(appMobile);
        this.testService.startHelloMessageExchange(appMobile, limitedAppMobileIds, appMobileMap);
        this.testService.stopHelloMessageExchanges(appMobile);

        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException("TODO: implement me");
    }

    @Given("{string} declare himself sick with a long code")
    public void declare_sick(String userName) {
        this.robertWsService.reportContactHistory();
        throw new io.cucumber.java.PendingException("TODO: implement me");
    }


}
