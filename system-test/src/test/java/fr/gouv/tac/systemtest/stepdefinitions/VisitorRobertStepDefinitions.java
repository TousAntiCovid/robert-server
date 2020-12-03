package fr.gouv.tac.systemtest.stepdefinitions;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.tac.robert.ApiException;
import fr.gouv.tac.robert.model.PushInfo;
import fr.gouv.tac.robert.model.RegisterRequest;
import fr.gouv.tac.robert.model.RegisterSuccessResponse;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import io.cucumber.java.en.Given;

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
        	RegisterSuccessResponse lastRegisterSuccessResponse = scenarioAppContext.getRobertApiInstance().register(registerRequest);
        	scenarioAppContext.getOrCreateVisitor(userName).setLastRegisterSuccessResponse(lastRegisterSuccessResponse);
        } catch (ApiException e) {
            System.err.println("Exception when calling RobertDefaultApi#register");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }
	    
	    
	}

	@Given("{string} scanned_covid_positive_QRCode")
	public void user_scanned_covid_positive_QRCode(String user) {
		scenarioAppContext.getOrCreateVisitor(user).sendTacReport(scenarioAppContext.getRobertApiInstance());
	}
	

	

}
