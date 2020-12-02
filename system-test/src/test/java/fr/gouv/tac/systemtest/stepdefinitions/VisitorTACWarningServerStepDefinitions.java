package fr.gouv.tac.systemtest.stepdefinitions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Objects;

import javax.inject.Inject;

import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.model.ExposureStatusRequest;
import fr.gouv.tac.tacwarning.model.ExposureStatusResponse;
import fr.gouv.tac.tacwarning.model.VisitToken;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class VisitorTACWarningServerStepDefinitions {

	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public VisitorTACWarningServerStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}

	@When("{string} asks for exposure status")
	public void asks_for_exposure_status(String user) {

		ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
		for (VisitToken token : scenarioAppContext.getRecordedUserVisitorMap().get(user).getTokens()) {
			exposureStatusRequest.addVisitTokensItem(token);
		}
		try {
			ExposureStatusResponse result = scenarioAppContext.getTacwApiInstance().eSR(exposureStatusRequest);
			scenarioAppContext.getLastExposureStatusResponseMap().put(user, result);
		} catch (ApiException e) {
			System.err.println("Exception when calling TacWarningDefaultApi#eSR");
			System.err.println("Status code: " + e.getCode());
			System.err.println("Reason: " + e.getResponseBody());
			System.err.println("Response headers: " + e.getResponseHeaders());
			e.printStackTrace();
		}

	}

	@Then("Exposure status should reports {string} as not being at risk")
	public void status_should_reports_as_not_being_at_risk(String user) {
		assertEquals(1, scenarioAppContext.getLastExposureStatusResponseMap().size());
		assertFalse(scenarioAppContext.getLastExposureStatusResponseMap().get(user).getAtRisk());
	}

}
