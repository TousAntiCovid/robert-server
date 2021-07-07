package fr.gouv.stopc.robert.integrationtests.feature;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;

class IsItFriday {
    static String isItFriday(String today) {
        return "Friday".equals(today) ? "TGIF" : "Nope";
    }
}

public class RobertClientStepDefinitions {

//    private final ApplicationProperties applicationProperties;
//
//    public RobertClientStepDefinitions(final ApplicationProperties applicationProperties) {
//        this.applicationProperties = applicationProperties;
//    }

    private String today;
    private String actualAnswer;

    @Given("today is {string}")
    public void today_is(String today) {
        this.today = today;
    }

    @When("I ask whether it's Friday yet")
    public void i_ask_whether_it_s_Friday_yet() {
        actualAnswer = IsItFriday.isItFriday(today);
    }

    @When("call")
    public void call() {
        final String reportUrl = new String("http://localhost:8086/api/v5").concat("/api/clea/v1/wreport");
        Response res = given()
                .when()
                .get("https://google.fr");
        assertEquals(  200, res.statusCode() /*actual value*/);
    }

    @Then("I should be told {string}")
    public void i_should_be_told(String expectedAnswer) {
//        i_should_be_told(this.applicationProperties.getBaseUrl());
        assertEquals(expectedAnswer, actualAnswer);
    }
}
