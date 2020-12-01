package fr.gouv.tac.systemtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import javax.inject.Inject;

import fr.gouv.tac.tacwarning.ApiException;
import fr.gouv.tac.tacwarning.model.ExposureStatusRequest;
import fr.gouv.tac.tacwarning.model.ExposureStatusResponse;
import fr.gouv.tac.tacwarning.model.VisitToken;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class VisitStepDefinitions {

	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public VisitStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}

	@Given("{string} recorded a visit to the {string} {string} at {int}:{int}, {int} days ago")
	public void user_recorded_a_visit_to_venue_at(String userName, String venue, String venueName, int hour,
			int minutes, int dayBeforeToday) {
		LocalDateTime dateTime = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
		LocalDateTime dateTime2 = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(2);
		dateTime2 = dateTime2.withHour(hour).withMinute(minutes).withSecond(0);
		Long timestamp = java.time.Duration.between(dateTime, dateTime2).getSeconds();

		timestamp = timestamp - (timestamp % ServerConfigUtil.TIME_ROUNDING);

		Visitor userVisitor = scenarioAppContext.getRecordedUserVisitorMap().get(userName);
		if (userVisitor == null) {
			userVisitor = new Visitor();
			userVisitor.setName(userName);
			scenarioAppContext.getRecordedUserVisitorMap().put(userName, userVisitor);
		}
		Place place = new Place(venueName);
		userVisitor.addVisit(place.getQrCode(), timestamp);

	}

	@When("{string} asks for exposure status")
	public void asks_for_exposure_status(String user) {

		ExposureStatusRequest exposureStatusRequest = new ExposureStatusRequest();
		for (VisitToken token : scenarioAppContext.getRecordedUserVisitorMap().get(user).getTokens()) {
			exposureStatusRequest.addVisitTokensItem(token);
		}
		try {
			ExposureStatusResponse result = scenarioAppContext.tacwApiInstance.eSR(exposureStatusRequest);
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
