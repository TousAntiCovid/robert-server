package fr.gouv.tac.systemtest.stepdefinitions;

import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.tac.systemtest.DummyMain4Tests;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import io.cucumber.java.en.Given;

public class PlaceTACWarningStepDefinitions {

	private static Logger logger = LoggerFactory.getLogger(PlaceTACWarningStepDefinitions.class);
	
	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public PlaceTACWarningStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}

	@Given("{string} created a static QRCode {string} with default values")
	public void created_a_static_qr_code_as_a_with_a_capacity_of_and_category(String venue, String qrCodeId) {
	    scenarioAppContext.getOrCreatePlace(venue).generateNewStaticQRCode(qrCodeId);
	}
	
	@Given("{string} created a static QRCode {string} as a {string} with a capacity of {int} and category {string}")
	public void created_a_static_qr_code_as_a_with_a_capacity_of_and_category(String venue, String qrCodeId, String venueType, Integer capacity, String category) {
		scenarioAppContext.getOrCreatePlace(venue).generateNewStaticQRCode(qrCodeId, venueType, capacity, category);
	}
}
