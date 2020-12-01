package fr.gouv.tac.systemtest;

import fr.gouv.tac.tacwarning.ApiClient;
import fr.gouv.tac.tacwarning.api.DefaultApi;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;

public class StepDefinitions {

    Visitors visitors = new Visitors();
    Places places = new Places();
    DefaultApi tacWarningApiInstance;
    ApiClient tacWarningDefaultClient;
    fr.gouv.tac.robert.api.DefaultApi tacApiInstance;
    fr.gouv.tac.robert.ApiClient tacDefaultClient;
    List<WhoWhereWhenHow> steps;


    void setUp(){
        
        tacWarningDefaultClient = fr.gouv.tac.tacwarning.Configuration.getDefaultApiClient();
        String tacWarningServerPath = Config.getProperty("TACW_BASE_URL");
        tacWarningServerPath = tacWarningServerPath+ "/"+Config.getProperty("TACW_VERSION");

        tacWarningDefaultClient.setBasePath(tacWarningServerPath);

        tacWarningApiInstance = new DefaultApi(tacWarningDefaultClient);


        tacDefaultClient = fr.gouv.tac.robert.Configuration.getDefaultApiClient();
        String tacServerPath = Config.getProperty("ROBERT_BASE_URL");
        tacServerPath = tacServerPath+ "/"+Config.getProperty("ROBERT_VERSION");

        tacDefaultClient.setBasePath(tacServerPath);

        tacApiInstance = new fr.gouv.tac.robert.api.DefaultApi(tacDefaultClient);
    }


    @Given("I have the following visits in the tac_warning")
    public void i_have_the_following_visits_in_the_tac_warning(DataTable dataTable) {

        this.setUp();

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
                    currentPlace.getQrCode(),
                    TimeUtil.naturalLanguageDateStringToTimestamp(step.getWhen()));
            currentVisitor.tacRobertRegister(tacApiInstance);
        }

    }

    @When("Covid+ person have not reported covid test to TAC")
    public void covid_person_have_not_reported_covid_test_to_tac() {

    }

    @Then("Covid- person status from TAC-W is false")
    public void covid_person_status_from_tac_w_is_false() {
      for (Visitor visitor : visitors.getList()){
          assertFalse(visitor.sendTacWarningStatus(tacWarningApiInstance));
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
