package fr.gouv.tac.systemtest.stepdefinitions.tacw;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.systemtest.ScenarioAppContext;
import fr.gouv.tac.systemtest.User;
import fr.gouv.tac.systemtest.config.ServerConfigUtil;
import fr.gouv.tac.systemtest.model.Place;
import fr.gouv.tac.systemtest.utils.TimeUtil;
import io.cucumber.java.en.Given;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class UserAppStepDefinitions {

	private static Logger logger = LoggerFactory.getLogger(UserAppStepDefinitions.class);

	private final ScenarioAppContext scenarioAppContext;

	@Inject
	public UserAppStepDefinitions(ScenarioAppContext scenarioAppContext) {
		this.scenarioAppContext = Objects.requireNonNull(scenarioAppContext, "scenarioAppContext must not be null");
	}

	@Given("{string} recorded a visit to {string} at {string}")
	public void user_recorded_a_visit_to_venue_at(String userName, String venueName, String time) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {

		logger.debug(userName+".user_recorded_a_visit_to_venue_(\"+venueName+\")_at_(\"+time+\")");
		Long timestamp = TimeUtil.naturalLanguageDateStringToNTPTimestamp(time);

		timestamp = timestamp - (timestamp % ServerConfigUtil.getTimeRounding());

		User userUser = scenarioAppContext.getOrCreateUser(userName);
		Place place = scenarioAppContext.getOrCreatePlace(venueName);
		place.generateNewStaticQRCode("newcode");
		userUser.addVisit(place.getDefaultStaticQrCode(), timestamp);

	}
	
	@Given("{string} recorded a visit to {string} at {string} with static QRCode {string}")
	public void user_recorded_a_visit_to_venue_at_with_static_qrcode(String userName, String venueName, String time, String qrCodeId) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {

		logger.debug(userName+".user_recorded_a_visit_to_venue_("+venueName+")_at_("+time+")_with_static_qrcode("+qrCodeId+")");
		Long timestamp = TimeUtil.naturalLanguageDateStringToNTPTimestamp(time);
		timestamp = timestamp - (timestamp % ServerConfigUtil.getTimeRounding());

		User userUser = scenarioAppContext.getOrCreateUser(userName);
		Place place = scenarioAppContext.getOrCreatePlace(venueName);
		userUser.addVisit(place.getStaticQRCodeMap().get(qrCodeId), timestamp);

	}


}
