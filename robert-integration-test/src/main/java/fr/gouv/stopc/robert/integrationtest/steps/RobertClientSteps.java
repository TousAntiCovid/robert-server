package fr.gouv.stopc.robert.integrationtest.steps;

import java.io.IOException;

import fr.gouv.stopc.robert.integrationtest.service.RobertBatchService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RobertClientSteps {

    private final RobertBatchService robertBatchService;

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

    @Given("they spent enough time together to propagate infection")
    public void a_mobile_app_spend_enough_time_near_another_mobile_app_to_infect_it()
            throws IOException, InterruptedException {
        robertBatchService.triggerBatch(true);
    }

    @Given("they did not spend enough time together to propagate infection")
    public void a_mobile_app_did_not_spend_enough_time_near_another_mobile_app_to_infect_it()
            throws IOException, InterruptedException {
        robertBatchService.triggerBatch(false);
    }

}
