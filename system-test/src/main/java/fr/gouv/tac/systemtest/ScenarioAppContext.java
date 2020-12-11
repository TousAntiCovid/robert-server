package fr.gouv.tac.systemtest;

import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Context for a scenario store the status of a set of user devices
 */
public class ScenarioAppContext {

	protected fr.gouv.tac.robert.ApiClient robertClient;
	protected fr.gouv.tac.robert.api.DefaultApi robertApiInstance;

	protected fr.gouv.tac.tacwarning.ApiClient tacwClient;
	protected fr.gouv.tac.tacwarning.api.DefaultApi tacwApiInstance;

	// these map corresponds to data recorded on user devices (one user per key)
	protected HashMap<String, Visitor> visitorMap = new HashMap<>();
	// these map corresponds to data recorded on venue (one venue per key)
	protected HashMap<String, Place> placeMap = new HashMap<>();

	private static org.slf4j.Logger logger = LoggerFactory.getLogger(ScenarioAppContext.class);


	public void setTacwClient(fr.gouv.tac.tacwarning.ApiClient tacwClient) {
		this.tacwClient = tacwClient;
	}

	public ScenarioAppContext() {
		this.robertClient = fr.gouv.tac.robert.Configuration.getDefaultApiClient();
		this.robertClient.setBasePath(ServerConfigUtil.getRobertServerPath());
		this.robertClient.setConnectTimeout(20000);
		this.robertApiInstance = new fr.gouv.tac.robert.api.DefaultApi(robertClient);
		System.out.println("RobertServerPath=" + ServerConfigUtil.getRobertServerPath());

		this.tacwClient = fr.gouv.tac.tacwarning.Configuration.getDefaultApiClient();
		this.tacwClient.setConnectTimeout(20000);
		this.tacwClient.setBasePath(ServerConfigUtil.getTACWarningServerPath());
		this.tacwApiInstance = new fr.gouv.tac.tacwarning.api.DefaultApi(tacwClient);
		System.out.println("TACWarningServerPath=" + ServerConfigUtil.getTACWarningServerPath());
	}

	public fr.gouv.tac.robert.ApiClient getRobertClient() {
		return robertClient;
	}

	public fr.gouv.tac.robert.api.DefaultApi getRobertApiInstance() {
		return robertApiInstance;
	}

	public fr.gouv.tac.tacwarning.ApiClient getTacwClient() {
		return tacwClient;
	}

	public fr.gouv.tac.tacwarning.api.DefaultApi getTacwApiInstance() {
		return tacwApiInstance;
	}

	public HashMap<String, Visitor> getRecordedUserVisitorMap() {
		return visitorMap;
	}

	public Visitor getOrCreateVisitor(String name) {
		Visitor result =visitorMap.get(name);
		if ( result == null) {
			logger.info("Creating visitor " + name);
			result = new Visitor(name);
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
			logger.info("Creating place " + name);
			result = new Place(name);
			placeMap.put(name, result);
		}
		return result;
	}
}