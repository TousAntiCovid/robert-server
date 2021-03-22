package fr.gouv.tac.systemtest.config;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.util.Properties;

@Slf4j
@UtilityClass
public class DockerConfig {

    private static final Properties loadedProperties = new Properties();

    public static final String ROBERT_SERVER_BATCH_IMAGE_NAME_KEY = "ROBERT_SERVER_BATCH_IMAGE_NAME";
    public static final String ROBERT_SERVER_BATCH_IMAGE_VERSION_KEY = "ROBERT_SERVER_BATCH_IMAGE_VERSION";
    public static final String DOCKERD_ADDRESS_KEY = "DOCKERD_ADDRESS";
    public static final String ROBERT_SERVER_BATCH_CONTAINER_NAME_KEY = "ROBERT_SERVER_BATCH_CONTAINER_NAME";

    private final String configFilePath = "/docker/";

    static {
        try {
            final String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
            final String appConfigPath = rootPath + configFilePath + "docker-config.properties";
            loadedProperties.load(new FileInputStream(appConfigPath));
            log.info("loaded properties: {}", loadedProperties.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return loadedProperties.get(key).toString();
    }
}
