package fr.gouv.tac.systemtest.config;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@UtilityClass
public class RobertServerBatchConfig {

    public static final String ROBERT_PROTOCOL_SCORING_THRESHOLD_KEY = "ROBERT_PROTOCOL_SCORING_THRESHOLD";
    public static final String ROBERT_SCORING_BATCH_MODE_KEY = "ROBERT_SCORING_BATCH_MODE";

    private static final Properties loadedProperties = new Properties();

    private final String configFilePath = "/robert-server-batch/";

    static {
        try {
            final String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
            final String appConfigPath = rootPath + configFilePath + "env.properties";
            loadedProperties.load(new FileInputStream(appConfigPath));
            log.info("loaded properties: {}", loadedProperties.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getPropertiesAsList() {
        return loadedProperties.entrySet()
                .stream()
                .map(entry -> String.join("=", entry.getKey().toString(), entry.getValue().toString()))
                .collect(Collectors.toList());
    }

    public static String getProperty(String key) {
        return loadedProperties.get(key).toString();
    }
}
