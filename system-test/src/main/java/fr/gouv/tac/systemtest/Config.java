package fr.gouv.tac.systemtest;

import java.io.FileInputStream;
import java.util.Properties;

public class Config {
    private static Properties defaultProps = new Properties();
    static {
        try {
            String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
            String appConfigPath = rootPath + "config.properties";
            FileInputStream in = new FileInputStream(appConfigPath);
            defaultProps.load(in);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String getProperty(String key) {
        return defaultProps.getProperty(key);
    }
}