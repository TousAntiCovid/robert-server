package fr.gouv.stopc.robert.e2e.steps;

import fr.gouv.stopc.robert.e2e.config.ApplicationProperties;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RobertClientSteps {

    @Given("{string} has the application TAC")
    public void generate_user(String userName) {
        log.info("TODO : generate mobile and user");
    }

    @Given("{string} registered on TAC with the Captcha service")
    public void request_and_resolve_captcha_challenge(String userName) {
        log.info("TODO : generate");
    }

    @Then("{string} is registed on TAC")
    public void  register_user_on_TAC(String userName) {
        log.info("TODO : generate");
    }

}
