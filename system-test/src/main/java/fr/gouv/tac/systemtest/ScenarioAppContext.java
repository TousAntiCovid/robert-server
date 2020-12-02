package fr.gouv.tac.systemtest;

import java.util.HashMap;

import fr.gouv.tac.robert.model.RegisterSuccessResponse;
import fr.gouv.tac.tacwarning.model.ExposureStatusResponse;

/**
 * Context for a scenario store the status of a set of user devices
 */
public class ScenarioAppContext {

	protected fr.gouv.tac.robert.ApiClient robertClient;
	protected fr.gouv.tac.robert.api.DefaultApi robertApiInstance;

	protected fr.gouv.tac.tacwarning.ApiClient tacwClient;
	protected fr.gouv.tac.tacwarning.api.DefaultApi tacwApiInstance;

	// these maps corresponds to data recorded on user devices (one user per key)
	protected HashMap<String, RegisterSuccessResponse> lastRegisterSuccessResponseMap = new HashMap<>();
	protected HashMap<String, ExposureStatusResponse> lastExposureStatusResponseMap = new HashMap<>();
	protected HashMap<String, Visitor> visitorMap = new HashMap<>();
	protected HashMap<String, Place> placeMap = new HashMap<>();


	public void setTacwClient(fr.gouv.tac.tacwarning.ApiClient tacwClient) {
		this.tacwClient = tacwClient;
	}

	public ScenarioAppContext() {
		this.robertClient = fr.gouv.tac.robert.Configuration.getDefaultApiClient();
		this.robertClient.setBasePath(ServerConfigUtil.getRobertServerPath());
		this.robertApiInstance = new fr.gouv.tac.robert.api.DefaultApi(robertClient);
		System.out.println("RobertServerPath=" + ServerConfigUtil.getRobertServerPath());

		this.tacwClient = fr.gouv.tac.tacwarning.Configuration.getDefaultApiClient();
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

	public HashMap<String, RegisterSuccessResponse> getLastRegisterSuccessResponseMap() {
		return lastRegisterSuccessResponseMap;
	}

	public HashMap<String, ExposureStatusResponse> getLastExposureStatusResponseMap() {
		return lastExposureStatusResponseMap;
	}

	public HashMap<String, Visitor> getRecordedUserVisitorMap() {
		return visitorMap;
	}
	public Visitor getOrCreateVisitor(String name) {
		Visitor result =visitorMap.get(name);
		if ( result == null) {
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
			result = new Place(name);
			placeMap.put(name, result);
		}
		return result;
	}
}
