package fr.gouv.tac.systemtest;

import java.util.Objects;

import javax.inject.Inject;

import fr.gouv.tac.robert.ApiClient;
import fr.gouv.tac.robert.Configuration;
import fr.gouv.tac.robert.api.DefaultApi;
import fr.gouv.tac.robert.model.PushInfo;
import fr.gouv.tac.robert.model.RegisterRequest;
import fr.gouv.tac.robert.ApiException;
import io.cucumber.java.en.Given;
import io.cucumber.guice.ScenarioScoped;

@ScenarioScoped
public class RegisterStepDefinitions {

	private final ScenarioAppContext scenarioAppContext;
	
	@Inject
	public RegisterStepDefinitions(ScenarioAppContext scenarioAppContext) {
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
        	scenarioAppContext.getLastRegisterSuccessResponseMap().put(userName, scenarioAppContext.robertApiInstance.register(registerRequest));
        } catch (ApiException e) {
            System.err.println("Exception when calling RobertDefaultApi#register");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
            throw e;
        }
	    
	    
	}
	

}
