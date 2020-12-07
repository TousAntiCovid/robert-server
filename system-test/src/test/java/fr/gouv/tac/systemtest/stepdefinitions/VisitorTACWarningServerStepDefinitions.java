package fr.gouv.tac.systemtest.stepdefinitions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.model.ExposureStatusRequest;
import fr.gouv.tac.tacwarning.model.ExposureStatusResponse;
import fr.gouv.tac.tacwarning.model.ReportRequest;
import fr.gouv.tac.tacwarning.model.VisitToken;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class VisitorTACWarningServerStepDefinitions {

	private static Logger logger = LoggerFactory.getLogger(VisitorTACWarningServerStepDefinitions.class);

	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public VisitorTACWarningServerStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}
	
	
	@Given("{string} reported to TACWarning a valid covid19 positive QRCode")
	public void reported_to_tac_warning_a_valid_covid19_positive_qr_code(String userName) {
		scenarioAppContext.getOrCreateVisitor(userName).sendTacWarningReport(scenarioAppContext.getTacwApiInstance());
	}

	@When("{string} asks for exposure status")
	public void asks_for_exposure_status(String user) {
		scenarioAppContext.getRecordedUserVisitorMap().get(user).sendTacWarningStatus(scenarioAppContext.getTacwApiInstance());
	}

	@Then("Exposure status should reports {string} as not being at risk")
	public void status_should_reports_as_not_being_at_risk(String user) {
		assertFalse(scenarioAppContext.getOrCreateVisitor(user).getLastExposureStatusResponse().getAtRisk());
	}
	
	@Then("Exposure status should reports {string} as being at risk")
	public void exposure_status_should_reports_as_being_at_risk(String user) {
		assertTrue(scenarioAppContext.getOrCreateVisitor(user).getLastExposureStatusResponse().getAtRisk());
	}

}
