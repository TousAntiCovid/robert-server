package fr.gouv.tac.systemtest.stepdefinitions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.tac.systemtest.Place;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.systemtest.ServerConfigUtil;
import fr.gouv.tac.systemtest.Visitor;
import io.cucumber.java.en.Given;

public class VisitorTACWarningAppStepDefinitions {

	private static Logger logger = LoggerFactory.getLogger(VisitorTACWarningAppStepDefinitions.class);

	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public VisitorTACWarningAppStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}

	@Given("{string} recorded a visit to {string} at {int}:{int}, {int} days ago")
	public void user_recorded_a_visit_to_venue_at(String userName, String venue, String venueName, int hour,
			int minutes, int dayBeforeToday) {
		LocalDateTime dateTime = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
		LocalDateTime dateTime2 = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(2);
		dateTime2 = dateTime2.withHour(hour).withMinute(minutes).withSecond(0);
		Long timestamp = java.time.Duration.between(dateTime, dateTime2).getSeconds();

		timestamp = timestamp - (timestamp % ServerConfigUtil.getTimeRounding());

		Visitor userVisitor = scenarioAppContext.getOrCreateVisitor(userName);
		Place place = scenarioAppContext.getOrCreatePlace(venueName);
		place.generateNewStaticQRCode("newcode");
		userVisitor.addVisit(place.getDefaultStaticQrCode(), timestamp);

	}
	
	@Given("{string} recorded a visit to {string} at {int}:{int}, {int} days ago with static QRCode {string}")
	public void user_recorded_a_visit_to_venue_at(String userName, String venueName, int hour,
			int minutes, int dayBeforeToday, String qrCodeId) {
		LocalDateTime dateTime = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
		LocalDateTime dateTime2 = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(2);
		dateTime2 = dateTime2.withHour(hour).withMinute(minutes).withSecond(0);
		Long timestamp = java.time.Duration.between(dateTime, dateTime2).getSeconds();

		timestamp = timestamp - (timestamp % ServerConfigUtil.getTimeRounding());

		Visitor userVisitor = scenarioAppContext.getOrCreateVisitor(userName);
		Place place = scenarioAppContext.getOrCreatePlace(venueName);
		place.generateNewStaticQRCode("newcode");
		userVisitor.addVisit(place.getStaticQRCodeMap().get(qrCodeId), timestamp);

	}


}
