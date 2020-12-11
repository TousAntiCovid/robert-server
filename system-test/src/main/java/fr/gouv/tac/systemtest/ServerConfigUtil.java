package fr.gouv.tac.systemtest;

public class ServerConfigUtil {

	public static final int SALT = 1000;
	
	public static String getRobertServerPath() {
		String roberServerPath = Config.getProperty("ROBERT_BASE_URL","http://127.0.0.1:8086/api");
        String roberServerVersion = Config.getProperty("ROBERT_VERSION","v4");
        roberServerPath = roberServerPath+ "/" + roberServerVersion;
        return roberServerPath;
	}
	
	public static String getRobertServerRegisterURL() {
        String roberServerRegisterURL = getRobertServerPath()+ "/register";
        return roberServerRegisterURL;
	}
	
	public static String getTACWarningServerPath() {
		String tacWarningServerPath = Config.getProperty("TACW_BASE_URL");
        tacWarningServerPath = tacWarningServerPath+ "/"+Config.getProperty("TACW_VERSION");
		
		String tacwServerPath = Config.getProperty("TACW_BASE_URL", "http://127.0.0.1:8088/api/tac-warning");
        String tacwServerVersion = Config.getProperty("TACW_VERSION", "v1");
        tacwServerPath = tacwServerPath+ "/" + tacwServerVersion;
        return tacwServerPath;
	}
	
	public static Long getTimeRounding() {
		return Long.parseLong(Config.getProperty("TIME_ROUNDING"));
	}
	
}