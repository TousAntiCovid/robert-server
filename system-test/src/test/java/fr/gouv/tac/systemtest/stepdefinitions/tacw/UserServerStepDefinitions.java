package fr.gouv.tac.systemtest.stepdefinitions.tacw;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.systemtest.stepdefinitions.RiskLevel;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class UserServerStepDefinitions {

	private static Logger logger = LoggerFactory.getLogger(UserServerStepDefinitions.class);

	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public UserServerStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}
	
	
	@Given("{string} reported to TACWarning a valid covid19 positive QRCode")
	public void reported_to_tac_warning_a_valid_covid19_positive_qr_code(String userName) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
		scenarioAppContext.getOrCreateUser(userName).sendTacWarningReport(scenarioAppContext.getTacwApiInstance());
	}

	@When("{string} asks for exposure status")
	public void asks_for_exposure_status(String user) {
		scenarioAppContext.getRecordedUserVisitorMap().get(user).sendTacWarningStatus(scenarioAppContext.getTacwApiInstance());
	}

	@Then("Exposure status should reports {string} as not being at risk")
	public void status_should_reports_as_not_being_at_risk(String user) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
		Assert.assertEquals(RiskLevel.NONE.getValue(),scenarioAppContext.getOrCreateUser(user).getLastExposureStatusResponse().getRiskLevel());
	}
	
	@Then("Exposure status should reports {string} as being at high level risk")
	public void exposure_status_should_reports_as_being_at_high_level_risk(String user) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
		assertEquals(RiskLevel.HIGH.getValue(), scenarioAppContext.getOrCreateUser(user).getLastExposureStatusResponse().getRiskLevel() );
	}


	@Then("Exposure status should reports {string} as being at low level risk")
	public void exposure_status_should_reports_as_being_at_low_level_risk(String user) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
		assertEquals(RiskLevel.LOW.getValue(), scenarioAppContext.getOrCreateUser(user).getLastExposureStatusResponse().getRiskLevel() );
	}

}