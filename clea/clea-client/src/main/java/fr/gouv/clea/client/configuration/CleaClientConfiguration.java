package fr.gouv.clea.client.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class CleaClientConfiguration {
    private static CleaClientConfiguration instance;
    private static String configFile = "application.properties";
    private Properties config;

    public CleaClientConfiguration() {
        this.config = new Properties();
    }

    /*
    * Returns input string with environment variable references expanded, e.g. $SOME_VAR or ${SOME_VAR}
    */
    private String resolveEnvVars(String input)
    {
        if (null == input)
        {
            return null;
        }
        // match ${ENV_VAR_NAME} or $ENV_VAR_NAME
        Pattern p = Pattern.compile("\\$\\{(\\w+):?(.+)?\\}");
        Matcher m = p.matcher(input); // get a matcher object
        if(m.matches()){
            String envVarName = m.group(1);
            String envVarValue = null == System.getenv(envVarName) ? m.group(2) : System.getenv(envVarName);
            return envVarValue;
        }
        return input;
    }

    public static CleaClientConfiguration getInstance() throws IOException {
        if (instance == null) {
            instance = new CleaClientConfiguration();
            instance.initialize();
        }

        return instance;
    }

    public void initialize() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
        if (inputStream != null) {
            this.config.load(inputStream);
        } else {
            throw new FileNotFoundException("config file '" + configFile + "' doesn't exists.");
        }
        inputStream.close();
    }

    public String getBackendUrl() {
        return this.resolveEnvVars(this.config.getProperty("backend_url", ""));
    }
    
    public String getReportPath() {
        return this.resolveEnvVars(this.config.getProperty("report_path", ""));
    }
    
    public String getStatusPath() {
        return this.resolveEnvVars(this.config.getProperty("status_path", ""));
    }

    public String getIndexFilename(){
        return this.resolveEnvVars(this.config.getProperty("index_filename",""));
    }
    
    public String getQrPrefix() {
        return this.resolveEnvVars(this.config.getProperty("qrprefix", ""));
    }


    public int getDurationUnitInSeconds(){
        try{
            return Integer.parseInt(this.resolveEnvVars(this.config.getProperty("duration_unit", "")));
        }catch(NumberFormatException e){
            return 0;
        }
    }
    
    public int getDupScanThreshold() {
        try {
            return Integer.parseInt(this.resolveEnvVars(this.config.getProperty("dup_scan_threshold", "")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getBatchTriggerUrl() {
        return this.resolveEnvVars(this.config.getProperty("batch_trigger_url", ""));
    }
}
