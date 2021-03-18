package fr.gouv.tac.systemtest.stepdefinitions.tacw;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.robert.ApiException;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.systemtest.User;
import fr.gouv.tac.systemtest.model.Place;
import fr.gouv.tac.systemtest.model.Places;
import fr.gouv.tac.systemtest.model.Visitors;
import fr.gouv.tac.systemtest.stepdefinitions.RiskLevel;
import fr.gouv.tac.systemtest.utils.TimeUtil;
import fr.gouv.tac.systemtest.utils.WhoWhereWhenHow;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.inject.Inject;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StepDefinitions {

    Visitors visitors = new Visitors();
    Places places = new Places();
    List<WhoWhereWhenHow> steps;

	private final ScenarioAppContext scenarioAppContext;
	
	@Inject
	public StepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull( scenarioAppContext, "scenarioAppContext must not be null" );
	}

    @Given("I have the following visits in the tac_warning")
    public void i_have_the_following_visits_in_the_tac_warning(DataTable dataTable) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, ApiException {


        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        steps = new ArrayList<>();
        for (Map<String, String> columns : rows) {
            steps.add(new
                    WhoWhereWhenHow(
                            columns.get("who"),
                            columns.get("where"),
                            columns.get("when"),
                            columns.get("covidStatus"),
                            columns.get("outcome")
                    )
            );
        }

        User currentUser;
        Place currentPlace;
        for (WhoWhereWhenHow step : steps){
            currentUser = visitors.getUserByName(step.getWho());
            currentUser.setCovidStatus(step.getCovidStatus());
            currentUser.setOutcome(step.getOutcome());
            currentPlace = places.getPlaceByName(step.getWhere());
            currentUser.addVisit(
                    currentPlace.getDefaultStaticQrCode(),
                    TimeUtil.naturalLanguageDateStringToNTPTimestamp(step.getWhen()));
            currentUser.tacRobertRegister(scenarioAppContext.getRobertApiInstance());
        }

    }

    @When("Covid+ person have not reported covid test to TAC")
    public void covid_person_have_not_reported_covid_test_to_tac() {

    }

    @Then("Covid- person status from TAC-W is not at risk")
    public void covid_person_status_from_tac_w_is_not_at_risk() {
      for (User user : visitors.getList()){
          assertEquals(RiskLevel.NONE.getValue(), user.sendTacWarningStatus(scenarioAppContext.getTacwApiInstance()));
      }
    }

    @When("Covid+ person report to TAC and TAC-W")
    public void covid_person_report_to_tac_and_tac_w() throws ApiException {
        for (User user : visitors.getList()){
        	if(user.getCovidStatus()) {
        		assertTrue(user.sendRobertReportBatch("string", scenarioAppContext.getRobertApiInstance()).getSuccess());
            	assertTrue(user.sendTacWarningReport(scenarioAppContext.getTacwApiInstance()));
        	}
        }
    }

    @Then("Covid- person status from TAC-W is at high level risk")
    public void covid_person_status_from_tac_w_is_at_high_level_risk() {
        for (User user : visitors.getList()){
                assertEquals(RiskLevel.HIGH.getValue(), user.sendTacWarningStatus(scenarioAppContext.getTacwApiInstance()));
        }
    }
}
