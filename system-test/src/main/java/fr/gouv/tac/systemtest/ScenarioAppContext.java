package fr.gouv.tac.systemtest;

import fr.gouv.stopc.robert.server.crypto.exception.RobertServerCryptoException;
import fr.gouv.tac.submission.code.server.api.GenerateCodeApi;
import fr.gouv.tac.submission.code.server.api.VerifyCodeApi;
import fr.gouv.tac.systemtest.config.ServerConfigUtil;
import fr.gouv.tac.systemtest.model.Place;
import lombok.extern.slf4j.Slf4j;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Context for a scenario store the status of a set of user devices
 */
@Slf4j
public class ScenarioAppContext {

	protected fr.gouv.tac.robert.ApiClient robertClient;
	protected fr.gouv.tac.robert.api.DefaultApi robertApiInstance;

	protected fr.gouv.tac.submission.code.server.ApiClient submissionCodeServerApiClient;
	protected fr.gouv.tac.submission.code.server.api.GenerateCodeApi generateCodeApiInstance;
	protected fr.gouv.tac.submission.code.server.api.VerifyCodeApi verifyCodeApiInstance;

	protected fr.gouv.tac.tacwarning.ApiClient tacwClient;
	protected fr.gouv.tac.tacwarning.api.DefaultApi tacwApiInstance;

	// these map corresponds to data recorded on user devices (one user per key)
	protected HashMap<String, User> visitorMap = new HashMap<>();
	// these map corresponds to data recorded on venue (one venue per key)
	protected HashMap<String, Place> placeMap = new HashMap<>();

	public void setTacwClient(fr.gouv.tac.tacwarning.ApiClient tacwClient) {
		this.tacwClient = tacwClient;
	}

	public ScenarioAppContext() {
		this.robertClient = fr.gouv.tac.robert.Configuration.getDefaultApiClient();
		this.robertClient.setBasePath(ServerConfigUtil.getRobertServerPath());
		this.robertClient.setConnectTimeout(20000);
		this.robertApiInstance = new fr.gouv.tac.robert.api.DefaultApi(robertClient);
		log.info("RobertServerPath=" + ServerConfigUtil.getRobertServerPath());

		this.submissionCodeServerApiClient = fr.gouv.tac.submission.code.server.Configuration.getDefaultApiClient();
		this.submissionCodeServerApiClient.setBasePath(ServerConfigUtil.getSubmissionCodeServerPath());
		this.submissionCodeServerApiClient.setConnectTimeout(20000);
		this.generateCodeApiInstance = new fr.gouv.tac.submission.code.server.api.GenerateCodeApi(submissionCodeServerApiClient);
		this.verifyCodeApiInstance = new fr.gouv.tac.submission.code.server.api.VerifyCodeApi(submissionCodeServerApiClient);
		log.info("SubmissionCodeServerPath=" + ServerConfigUtil.getSubmissionCodeServerPath());

		this.tacwClient = fr.gouv.tac.tacwarning.Configuration.getDefaultApiClient();
		this.tacwClient.setConnectTimeout(20000);
		this.tacwClient.setBasePath(ServerConfigUtil.getTACWarningServerPath());
		this.tacwApiInstance = new fr.gouv.tac.tacwarning.api.DefaultApi(tacwClient);
		log.info("TACWarningServerPath=" + ServerConfigUtil.getTACWarningServerPath());
	}

	public fr.gouv.tac.robert.ApiClient getRobertClient() {
		return robertClient;
	}

	public fr.gouv.tac.robert.api.DefaultApi getRobertApiInstance() {
		return robertApiInstance;
	}

	public GenerateCodeApi getGenerateCodeApiInstance() {
		return generateCodeApiInstance;
	}

	public VerifyCodeApi getVerifyCodeApiInstance() {
		return verifyCodeApiInstance;
	}

	public fr.gouv.tac.tacwarning.ApiClient getTacwClient() {
		return tacwClient;
	}

	public fr.gouv.tac.tacwarning.api.DefaultApi getTacwApiInstance() {
		return tacwApiInstance;
	}

	public HashMap<String, User> getRecordedUserVisitorMap() {
		return visitorMap;
	}

	public User getOrCreateUser(String name) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, RobertServerCryptoException {
		User result =visitorMap.get(name);
		if ( result == null) {
			log.info("Creating visitor " + name);
			result = new User(name);
			visitorMap.put(name, result);
		}
		return result;
	}

	public HashMap<String, Place> getPlaceMap() {
		return placeMap;
	}
	
	public Place getOrCreatePlace(String name) {
		Place result =placeMap.get(name);
		if ( result == null) {
			log.info("Creating place " + name);
			result = new Place(name);
			placeMap.put(name, result);
		}
		return result;
	}
}
