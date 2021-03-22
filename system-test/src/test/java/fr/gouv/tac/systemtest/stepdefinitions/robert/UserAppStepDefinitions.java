package fr.gouv.tac.systemtest.stepdefinitions.robert;

import static org.junit.jupiter.api.Assertions.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.robert.api.DefaultApi;
import fr.gouv.tac.robert.model.ExposureStatusResponse;
import fr.gouv.tac.robert.model.PushInfo;
import fr.gouv.tac.robert.model.RegisterRequest;
import fr.gouv.tac.robert.model.RegisterSuccessResponse;
import fr.gouv.tac.robert.model.ReportBatchResponse;
import fr.gouv.tac.robert.model.SuccessResponse;
import fr.gouv.tac.submission.code.server.ApiException;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.systemtest.User;
import fr.gouv.tac.systemtest.stepdefinitions.RiskLevel;
import fr.gouv.tac.systemtest.utils.DockerUtils;
import fr.gouv.tac.systemtest.utils.TacSystemTestException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

@Slf4j
public class UserAppStepDefinitions {

    private final DockerUtils dockerUtils;

    private final ScenarioAppContext scenarioAppContext;

    private final String SUCCESSFUL_OPERATION_MESSAGE = "Successful operation";

    @Inject
    public UserAppStepDefinitions(final ScenarioAppContext scenarioAppContext) {
        this.scenarioAppContext = scenarioAppContext;
        this.dockerUtils = new DockerUtils();
    }

    @Given("{string} scanned covid positive QRCode")
    public void user_scanned_covid_positive_QRCode(String user) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.submission.code.server.ApiException, fr.gouv.tac.robert.ApiException {
        final String shortCode = scenarioAppContext.getGenerateCodeApiInstance().generate().getCode();
        scenarioAppContext.getOrCreateUser(user).sendRobertReportBatch(shortCode, scenarioAppContext.getRobertApiInstance());
    }

    @Given("{string} registered on TAC")
    public void user_registered_on_tac(String userName) throws fr.gouv.tac.robert.ApiException {

        final RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setCaptcha("string");
        registerRequest.setCaptchaId("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        registerRequest.setClientPublicECDHKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEB+Q03HmTHYPpHUs3UZIcY0robfRuP0zIVwItwseq8JMCl8W9yCuVRyFGTqL7VqnhZN1tQqS4nwbEW4FSK/JLbg==");
        PushInfo pushInfo = new PushInfo();
        pushInfo.setLocale("fr");
        pushInfo.setTimezone("Europe/Paris");
        pushInfo.setToken("string");
        registerRequest.pushInfo(pushInfo);

        try {
            final User user = scenarioAppContext.getOrCreateUser(userName);
            RegisterSuccessResponse lastRegisterSuccessResponse = user.tacRobertRegister(scenarioAppContext.getRobertApiInstance());

            user.setLastRegisterSuccessResponse(lastRegisterSuccessResponse);
        } catch (RobertServerCryptoException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    @Given("{string} reported positive to covid test via a doctor code")
    public void a_mobile_app_reported_positive(final String name1) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, ApiException, fr.gouv.tac.robert.ApiException {

        final User appMobileClient1 = scenarioAppContext.getOrCreateUser(name1);
        final DefaultApi robertApi = scenarioAppContext.getRobertApiInstance();

        // the submission code server must be running
        final String shortCode = scenarioAppContext.getGenerateCodeApiInstance().generate().getCode();

        final ReportBatchResponse reportBatchResponse = appMobileClient1.sendRobertReportBatch(shortCode, robertApi);
        assertEquals(SUCCESSFUL_OPERATION_MESSAGE, reportBatchResponse.getMessage());
        assertNotNull(reportBatchResponse.getReportValidationToken());

        // reports triggers unregister ??? ==> CURRENTLY THIS IS NOT THE CASE
    }

    @Given("Did not meet anyone")
    public void a_mobile_app_did_not_meet_anyone() throws TacSystemTestException {

        // lowest threshold leading to a systematic positive risk evaluation when anyone meets the infected person
        final boolean mustBeAtRisk = true;

        dockerUtils.launchRobertServerBatchContainer(DockerUtils.BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, mustBeAtRisk);
    }

    @Given("{string} met {string}")
    public void a_mobile_app_met_another_mobile_app(final String name1, final String name2) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final User appMobileClient1 = scenarioAppContext.getOrCreateUser(name1);
        final User appMobileClient2 = scenarioAppContext.getOrCreateUser(name2);

        appMobileClient1.exchangeEbIdWith(appMobileClient2);
    }

    @Given("They did not spend enough time together to propagate infection")
    public void a_mobile_app_did_not_spend_enough_time_near_another_mobile_app_to_infect_it() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, TacSystemTestException {
        final boolean mustBeAtRisk = false;

        dockerUtils.launchRobertServerBatchContainer(DockerUtils.BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, mustBeAtRisk);
    }

    @Given("They spent enough time together to propagate infection")
    public void andSpentEnoughTimeTogetherToPropagateInfection() throws TacSystemTestException {
        final boolean mustBeAtRisk = true;

        dockerUtils.launchRobertServerBatchContainer(DockerUtils.BatchMode.SCORE_CONTACTS_AND_COMPUTE_RISK, false, mustBeAtRisk);
    }

    @When("{string} requests exposure status")
    public void a_mobile_app_requests_status_exposure_endpoint(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.robert.ApiException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        final ExposureStatusResponse statusResponse = user.status(scenarioAppContext.getRobertApiInstance());
        user.setLastRobertExposureStatusResponse(statusResponse);
    }

    @When("{string} deletes his exposure history")
    public void a_mobile_app_deletes_its_exposure_history(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.robert.ApiException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        user.setLastDeleteHistoryResponse(user.deleteHistory(scenarioAppContext.getRobertApiInstance()));
    }

    @When("{string} unregisters from TAC")
    public void a_mobile_app_unregiers_from_tac(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        user.setLastUnregisterResponse(user.unregister(scenarioAppContext.getRobertApiInstance()));
    }

    @Then("{string} exposure status should report risk level superior to 0 and updated last contact date and last risk scoring date")
    public void a_mobile_app_exposure_status_response_should_report_positive_risk_level_and_updated_last_contact_date_and_last_risk_scoring_date(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final User appMobileClient = scenarioAppContext.getOrCreateUser(name);
        final ExposureStatusResponse statusResponse = appMobileClient.getLastRobertExposureStatusResponse();
        assertNotNull(statusResponse.getRiskLevel());
        Assertions.assertEquals(RiskLevel.HIGH.getValue(), statusResponse.getRiskLevel());
        assertNotNull(statusResponse.getLastContactDate());
        assertNotNull(statusResponse.getLastRiskScoringDate());
        assertNotNull(statusResponse.getDeclarationToken());
    }

    @Then("{string} exposure status should report risk level equal to 0")
    public void a_mobile_app_exposure_status_response_should_report_positive_risk_level_equal_to_0(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final User appMobileClient = scenarioAppContext.getOrCreateUser(name);
        final ExposureStatusResponse statusResponse = appMobileClient.getLastRobertExposureStatusResponse();
        assertNotNull(statusResponse.getRiskLevel());
        assertEquals(RiskLevel.NONE.getValue(), statusResponse.getRiskLevel());
        assertNull(statusResponse.getLastContactDate());
        assertNull(statusResponse.getLastRiskScoringDate());
        assertNull(statusResponse.getDeclarationToken());
    }

    @Then("{string} cannot call status endpoint")
    public void a_mobile_app_cannot_call_status_endpoint(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.robert.ApiException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        assertThrows(
                fr.gouv.tac.robert.ApiException.class, () -> user.status(scenarioAppContext.getRobertApiInstance())
        );
    }

    @Then("{string} cannot call delete history exposure endpoint")
    public void a_mobile_app_cannot_call_delete_history_exposure_endpoint(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        assertThrows(
                fr.gouv.tac.robert.ApiException.class, () -> user.deleteHistory(scenarioAppContext.getRobertApiInstance())
        );

    }

    @When("{string} deletes his exposure history from TAC")
    public void deletes_his_exposure_history_from_TAC(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.robert.ApiException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        user.setLastDeleteHistoryResponse(user.deleteHistory(scenarioAppContext.getRobertApiInstance()));
    }

    @Then("{string}'s application acknowledges a successful operation message for exposure history deletion")
    public void a_mobile_app_acknowledges_successful_operation_message_for_exposure_history_deletion(final String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
        final User user = scenarioAppContext.getOrCreateUser(name);
        final SuccessResponse lastDeleteHistoryResponse = user.getLastDeleteHistoryResponse();
        assertTrue(lastDeleteHistoryResponse.getSuccess());
    }
}
