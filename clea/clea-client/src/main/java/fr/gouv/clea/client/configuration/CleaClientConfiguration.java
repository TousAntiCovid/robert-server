package fr.gouv.clea.client.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CleaClientConfiguration {
    private static CleaClientConfiguration instance;
    private static String configFile = "application.properties";
    private Properties config;

    public CleaClientConfiguration() {
        this.config = new Properties();
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
        return this.config.getProperty("backend_url", "");
    }
    
    public String getReportPath() {
        return this.config.getProperty("report_path", "");
    }
    
    public String getStatusPath() {
        return this.config.getProperty("status_path", "");
    }

    public String getIndexFilename(){
        return this.config.getProperty("index_filename","");
    }
    
    public String getQrPrefix() {
        return this.config.getProperty("qrprefix", "");
    }


    public int getDurationUnitInSeconds(){
        try{
            return Integer.parseInt(this.config.getProperty("duration_unit", ""));
        }catch(NumberFormatException e){
            return 0;
        }
    }
    
    public int getDupScanThreshold() {
        try {
            return Integer.parseInt(this.config.getProperty("dup_scan_threshold", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
