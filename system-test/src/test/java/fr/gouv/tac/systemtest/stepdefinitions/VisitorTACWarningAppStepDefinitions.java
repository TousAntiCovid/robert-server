package fr.gouv.tac.systemtest.stepdefinitions;

import fr.gouv.tac.systemtest.*;
import io.cucumber.java.en.Given;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Objects;

public class VisitorTACWarningAppStepDefinitions {

	private static Logger logger = LoggerFactory.getLogger(VisitorTACWarningAppStepDefinitions.class);

	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public VisitorTACWarningAppStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}

	@Given("{string} recorded a visit to {string} at {string}")
	public void user_recorded_a_visit_to_venue_at(String userName, String venue, String venueName, String time) {

		Long timestamp = TimeUtil.naturalLanguageDateStringToNTPTimestamp(time);

		timestamp = timestamp - (timestamp % ServerConfigUtil.getTimeRounding());

		Visitor userVisitor = scenarioAppContext.getOrCreateVisitor(userName);
		Place place = scenarioAppContext.getOrCreatePlace(venueName);
		place.generateNewStaticQRCode("newcode");
		userVisitor.addVisit(place.getDefaultStaticQrCode(), timestamp);

	}
	
	@Given("{string} recorded a visit to {string} at {string} with static QRCode {string}")
	public void user_recorded_a_visit_to_venue_at_with_static_qrcode(String userName, String venueName, String time, String qrCodeId) {

		Long timestamp = TimeUtil.naturalLanguageDateStringToNTPTimestamp(time);
		timestamp = timestamp - (timestamp % ServerConfigUtil.getTimeRounding());

		Visitor userVisitor = scenarioAppContext.getOrCreateVisitor(userName);
		Place place = scenarioAppContext.getOrCreatePlace(venueName);
		place.generateNewStaticQRCode("newcode");
		userVisitor.addVisit(place.getStaticQRCodeMap().get(qrCodeId), timestamp);

	}


}
