package fr.gouv.tac.systemtest.stepdefinitions;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.robert.ApiException;
import fr.gouv.tac.robert.model.PushInfo;
import fr.gouv.tac.robert.model.RegisterRequest;
import fr.gouv.tac.robert.model.RegisterSuccessResponse;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.systemtest.User;
import io.cucumber.java.en.Given;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class VisitorRobertStepDefinitions {

	private static Logger logger = LoggerFactory.getLogger(VisitorRobertStepDefinitions.class);
	
	private final ScenarioAppContext scenarioAppContext;
	
	@Inject
	public VisitorRobertStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull( scenarioAppContext, "scenarioAppContext must not be null" );
	}
	
	@Given("{string} registered on TAC")
	public void user_registered_on_tac(String userName) throws ApiException {
		
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.captcha("string");
        registerRequest.captchaId("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        registerRequest.clientPublicECDHKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEB+Q03HmTHYPpHUs3UZIcY0robfRuP0zIVwItwseq8JMCl8W9yCuVRyFGTqL7VqnhZN1tQqS4nwbEW4FSK/JLbg==");
        PushInfo pushInfo = new PushInfo();
        pushInfo.locale("fr");
        pushInfo.timezone("Europe/Paris");
        pushInfo.token("string");
        registerRequest.pushInfo(pushInfo);
        
        try {
            final User user = scenarioAppContext.getOrCreateUser(userName);
            RegisterSuccessResponse lastRegisterSuccessResponse = user.tacRobertRegister(scenarioAppContext.getRobertApiInstance());

            user.setLastRegisterSuccessResponse(lastRegisterSuccessResponse);
        } catch (RobertServerCryptoException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    @Given("{string} scanned covid positive QRCode")
    public void user_scanned_covid_positive_QRCode(String user) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.submission.code.server.ApiException {
        String shortCode = scenarioAppContext.getGenerateCodeApiInstance().generate().getCode();
        scenarioAppContext.getOrCreateUser(user).sendRobertReportBatch(shortCode, scenarioAppContext.getRobertApiInstance());
	}
}
