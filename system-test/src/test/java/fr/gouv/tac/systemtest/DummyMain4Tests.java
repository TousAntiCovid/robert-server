package fr.gouv.tac.systemtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.tac.robert.ApiException;
import fr.gouv.tac.systemtest.stepdefinitions.PlaceTACWarningStepDefinitions;
import fr.gouv.tac.systemtest.stepdefinitions.VisitorRobertStepDefinitions;
import fr.gouv.tac.systemtest.stepdefinitions.VisitorTACWarningAppStepDefinitions;
import fr.gouv.tac.systemtest.stepdefinitions.VisitorTACWarningServerStepDefinitions;

public class DummyMain4Tests {

	private static Logger logger = LoggerFactory.getLogger(DummyMain4Tests.class);
	
	private final ScenarioAppContext scenarioAppContext = new ScenarioAppContext();
	PlaceTACWarningStepDefinitions placeTACWarningSD;
	VisitorRobertStepDefinitions visitorRobertSD;
	VisitorTACWarningAppStepDefinitions visitorTACWarningAppSD;
	VisitorTACWarningServerStepDefinitions visitorTACWarningSD;
	
	public static void main(String[] args) throws ApiException {
		new DummyMain4Tests().run();

	}

	private void run() throws ApiException {
		visitorRobertSD = new VisitorRobertStepDefinitions(scenarioAppContext);
		placeTACWarningSD = new PlaceTACWarningStepDefinitions(scenarioAppContext);
		visitorTACWarningAppSD = new VisitorTACWarningAppStepDefinitions(scenarioAppContext);
		visitorTACWarningSD = new VisitorTACWarningServerStepDefinitions(scenarioAppContext);
		
		System.out.println("created_a_static_qr_code_as_a_with_a_capacity_of_and_category");
		logger.info("\"created_a_static_qr_code_as_a_with_a_capacity_of_and_category\"");
		placeTACWarningSD.created_a_static_qr_code_as_a_with_a_capacity_of_and_category("Restaurant", "LunchServiceQRCode");
		
		System.out.println("user_registered_on_tac");
		visitorRobertSD.user_registered_on_tac("Steffen");
		
		System.out.println("user_scanned_covid_positive_QRCode");
		visitorRobertSD.user_scanned_covid_positive_QRCode("Steffen");
		
		System.out.println("user_recorded_a_visit_to_venue_at");
		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at("Steffen", "Restaurant", 12, 30, 1, "LunchServiceQRCode");
		
		System.out.println("user_recorded_a_visit_to_venue_at");
		visitorTACWarningAppSD.user_recorded_a_visit_to_venue_at("Steffen", "Restaurant", 17, 30, 1, "LunchServiceQRCode");
		
		System.out.println("reported_to_tac_warning_a_valid_covid19_positive_qr_code");
		visitorTACWarningSD.reported_to_tac_warning_a_valid_covid19_positive_qr_code("Steffen");
	}

}
