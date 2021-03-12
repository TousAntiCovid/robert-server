package fr.gouv.tac.systemtest;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.systemtest.stepdefinitions.MobileClientAppStepDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.tac.robert.ApiException;
import fr.gouv.tac.systemtest.stepdefinitions.PlaceTACWarningStepDefinitions;
import fr.gouv.tac.systemtest.stepdefinitions.VisitorRobertStepDefinitions;
import fr.gouv.tac.systemtest.stepdefinitions.VisitorTACWarningAppStepDefinitions;
import fr.gouv.tac.systemtest.stepdefinitions.VisitorTACWarningServerStepDefinitions;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

public class DummyMain4Tests {

	private static final Logger logger = LoggerFactory.getLogger(DummyMain4Tests.class);
	
	private final ScenarioAppContext scenarioAppContext = new ScenarioAppContext();
	PlaceTACWarningStepDefinitions placeTACWarningSD;
	VisitorRobertStepDefinitions visitorRobertSD;
	VisitorTACWarningAppStepDefinitions visitorTACWarningAppSD;
	VisitorTACWarningServerStepDefinitions visitorTACWarningSD;
	MobileClientAppStepDefinitions mobileClientAppSD;

	public DummyMain4Tests() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
	}

	public static void main(String[] args) throws ApiException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.submission.code.server.ApiException {
		new DummyMain4Tests().run();

	}

	private void run() throws ApiException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException, fr.gouv.tac.submission.code.server.ApiException {
		visitorRobertSD = new VisitorRobertStepDefinitions(scenarioAppContext);
		placeTACWarningSD = new PlaceTACWarningStepDefinitions(scenarioAppContext);
		visitorTACWarningAppSD = new VisitorTACWarningAppStepDefinitions(scenarioAppContext);
		visitorTACWarningSD = new VisitorTACWarningServerStepDefinitions(scenarioAppContext);
		mobileClientAppSD = new MobileClientAppStepDefinitions(scenarioAppContext);
		
	    
		
		
		System.out.println("created_a_static_qr_code_as_a_with_a_capacity_of_and_category");
		logger.info("\"created_a_static_qr_code_as_a_with_a_capacity_of_and_category\"");
		placeTACWarningSD.created_a_static_qr_code_as_a_with_a_capacity_of_and_category("ChezGusto", "LunchService");
		
		//Given "Hugo" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" with static QRCode "LunchService"
		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at_with_static_qrcode("Hugo", "ChezGusto","Yesterday at 12:30pm", "LunchService");
	    //Given "Stephanie" recorded a visit to "Chez Gusto" at "12:30, 2 days ago" with static QRCode "LunchService"
		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at_with_static_qrcode("Stephanie", "ChezGusto","Yesterday at 12:30pm", "LunchService");
		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at_with_static_qrcode("Stephanie2", "ChezGusto","Yesterday at 12:30pm", "LunchService");
		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at_with_static_qrcode("Stephanie3", "ChezGusto","Yesterday at 12:30pm", "LunchService");
	    //Given "Stephanie" scanned covid positive QRCode
		visitorRobertSD.user_scanned_covid_positive_QRCode("Stephanie");
	    //Given "Stephanie" reported to TACWarning a valid covid19 positive QRCode
		visitorTACWarningSD.reported_to_tac_warning_a_valid_covid19_positive_qr_code("Stephanie");
		visitorTACWarningSD.asks_for_exposure_status("Stephanie");

	    //Given "Stephanie" scanned covid positive QRCode
		visitorRobertSD.user_scanned_covid_positive_QRCode("Stephanie2");
	    //Given "Stephanie" reported to TACWarning a valid covid19 positive QRCode
		visitorTACWarningSD.reported_to_tac_warning_a_valid_covid19_positive_qr_code("Stephanie2");
		visitorTACWarningSD.asks_for_exposure_status("Stephanie2");

	    //When "Hugo" asks for exposure status
		visitorTACWarningSD.asks_for_exposure_status("Hugo");
	    //Then Exposure status should reports "Hugo" as being at risk
		logger.info(scenarioAppContext.getOrCreateUser("Hugo").getLastExposureStatusResponse().getRiskLevel().toString());

//		System.out.println("user_registered_on_tac");
//		visitorRobertSD.user_registered_on_tac("Steffen");
//		
//		System.out.println("user_scanned_covid_positive_QRCode");
//		visitorRobertSD.user_scanned_covid_positive_QRCode("Steffen");
//		
//		System.out.println("user_recorded_a_visit_to_venue_at");
//		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at("Steffen", "ChezGusto", "Yesterday at 12:30");
//		
//		System.out.println("user_recorded_a_visit_to_venue_at");
//		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at_with_static_qrcode("Steffen", "ChezGusto","Yesterday at 5:30pm", "LunchServiceQRCode");
//		
//		System.out.println("reported_to_tac_warning_a_valid_covid19_positive_qr_code");
//		visitorTACWarningSD.reported_to_tac_warning_a_valid_covid19_positive_qr_code("Steffen");
	}

}
