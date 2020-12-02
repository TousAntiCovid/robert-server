package fr.gouv.tac.systemtest;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

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
    public void i_have_the_following_visits_in_the_tac_warning(DataTable dataTable) {


        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        steps = new ArrayList<WhoWhereWhenHow>();
        for (Map<String, String> columns : rows) {
            steps.add(new
                    WhoWhereWhenHow(
                            columns.get("who"),
                            columns.get("where"),
                            columns.get("when"),
                            columns.get("covidStatus")
                    )
            );
        }

        Visitor currentVisitor;
        Place currentPlace;
        for (WhoWhereWhenHow step : steps){
            currentVisitor = visitors.getVisitorByName(step.getWho());
            currentVisitor.setCovidStatus(step.getCovidStatus());
            currentPlace = places.getPlaceByName(step.getWhere());
            currentVisitor.addVisit(
                    currentPlace.getDefaultStaticQrCode(),
                    TimeUtil.naturalLanguageDateStringToTimestamp(step.getWhen()));
            currentVisitor.tacRobertRegister(scenarioAppContext.getRobertApiInstance());
        }

    }

    @When("Covid+ person have not reported covid test to TAC")
    public void covid_person_have_not_reported_covid_test_to_tac() {

    }

    @Then("Covid- person status from TAC-W is false")
    public void covid_person_status_from_tac_w_is_false() {
      for (Visitor visitor : visitors.getList()){
          assertFalse(visitor.sendTacWarningStatus(scenarioAppContext.getTacwApiInstance()));
      }
    }

    @When("Covid+ person report to TAC and TAC-W")
    public void covid_person_report_to_tac_and_tac_w() {
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }
    @Then("Covid- person status from TAC-W is true")
    public void covid_person_status_from_tac_w_is_true() {
        // Write code here that turns the phrase above into concrete actions
        throw new io.cucumber.java.PendingException();
    }

}
