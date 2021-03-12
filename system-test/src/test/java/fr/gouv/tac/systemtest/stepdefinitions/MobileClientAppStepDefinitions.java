package fr.gouv.tac.systemtest.stepdefinitions;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.systemtest.utils.DockerUtils;
import fr.gouv.tac.robert.api.DefaultApi;
import fr.gouv.tac.robert.model.ExposureStatusResponse;
import fr.gouv.tac.robert.model.ReportBatchResponse;
import fr.gouv.tac.submission.code.server.ApiException;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.systemtest.User;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MobileClientAppStepDefinitions {

    private final DockerUtils dockerUtils;

    private final ScenarioAppContext scenarioAppContext;

    private final String SUCCESSFUL_OPERATION_MESSAGE = "Successful operation";

    @Inject
    public MobileClientAppStepDefinitions(final ScenarioAppContext scenarioAppContext) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        this.scenarioAppContext = scenarioAppContext;
        this.dockerUtils = new DockerUtils();
    }

    @Given("{string} reported positive to covid test via a doctor code")
    public void a_mobile_app_reported_positive(final String name1) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, ApiException {

        User appMobileClient1 = scenarioAppContext.getOrCreateUser(name1);
        DefaultApi robertApi = scenarioAppContext.getRobertApiInstance();

        // the submission code server must be running
        final String shortCode = scenarioAppContext.getGenerateCodeApiInstance().generate().getCode();

        final ReportBatchResponse reportBatchResponse = appMobileClient1.sendRobertReportBatch(shortCode, robertApi);
        assertEquals(SUCCESSFUL_OPERATION_MESSAGE, reportBatchResponse.getMessage());
        assertNotNull(reportBatchResponse.getReportValidationToken());

        // reports triggers unregister ??? ==> CURRENTLY THIS IS NOT THE CASE
    }

    @Given("Did not meet anyone")
    public void a_mobile_app_did_not_meet_anyone() {

        // lowest threshold leading to a systematic positive risk evaluation when anyone meets the infected person
        final boolean mustBeAtRisk = true;

        dockerUtils.launchRobertServerBatchContainer(DockerUtils.BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, mustBeAtRisk);
    }

    @Given("{string} met {string}")
    public void a_mobile_app_spent_enough_time_near_another_mobile_app_to_infect_it(final String name1, final String name2) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        User appMobileClient1 = scenarioAppContext.getOrCreateUser(name1);
        User appMobileClient2 = scenarioAppContext.getOrCreateUser(name2);

        appMobileClient1.exchangeEbIdWith(appMobileClient2);
    }

    @Given("They did not spend enough time together to propagate infection")
    public void a_mobile_app_did_not_spend_enough_time_near_another_mobile_app_to_infect_it() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final boolean mustBeAtRisk = false;

        dockerUtils.launchRobertServerBatchContainer(DockerUtils.BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, mustBeAtRisk);
    }

    @Given("They spent enough time together to propagate infection")
    public void andSpentEnoughTimeTogetherToPropagateInfection() {
        final boolean mustBeAtRisk = true;

        dockerUtils.launchRobertServerBatchContainer(DockerUtils.BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, mustBeAtRisk);
    }

    @When("{string} requests exposure status")
    public void a_mobile_app_requests_status_exposure_endpoint(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        final ExposureStatusResponse statusResponse = user.status(scenarioAppContext.getRobertApiInstance());
        user.setLastRobertExposureStatusResponse(statusResponse);
    }

    @Then("{string} exposure status should report risk level superior to 0 and updated last contact date and last risk scoring date")
    public void a_mobile_app_exposure_status_response_should_report_positive_risk_level_and_updated_last_contact_date_and_last_risk_scoring_date(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        User appMobileClient = scenarioAppContext.getOrCreateUser(name);
        ExposureStatusResponse statusResponse = appMobileClient.getLastRobertExposureStatusResponse();
        assertNotNull(statusResponse.getRiskLevel());
        assertEquals(RiskLevel.HIGH.getValue(), statusResponse.getRiskLevel());
        assertNotNull(statusResponse.getLastContactDate());
        assertNotNull(statusResponse.getLastRiskScoringDate());
        assertNotNull(statusResponse.getDeclarationToken());
    }

    @Then("{string} exposure status should report risk level equal to 0")
    public void a_mobile_app_exposure_status_response_should_report_positive_risk_level_equal_to_0(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        User appMobileClient = scenarioAppContext.getOrCreateUser(name);
        ExposureStatusResponse statusResponse = appMobileClient.getLastRobertExposureStatusResponse();
        assertNotNull(statusResponse.getRiskLevel());
        assertEquals(RiskLevel.NONE.getValue(), statusResponse.getRiskLevel());
        assertNull(statusResponse.getLastContactDate());
        assertNull(statusResponse.getLastRiskScoringDate());
        assertNull(statusResponse.getDeclarationToken());
    }
}
