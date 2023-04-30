package shpp.level2.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class Config {
    private final Properties properties;
    private static final String PROPERTIES_FILE_NAME = "app.properties";

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    public Config() {
        properties = new Properties();

        loadPropertiesFromClasspath();

        if(properties.isEmpty()) {
            setDefaultProperties();
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    private void loadPropertiesFromClasspath() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE_NAME)
        ) {
            if (inputStream != null) {
                logger.debug("Load properties from '{}' file.", PROPERTIES_FILE_NAME);
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }else{
                throw new IOException(PROPERTIES_FILE_NAME);
            }
        } catch (IOException e) {
            logger.warn("Can't get properties from file {}.", PROPERTIES_FILE_NAME, e);
        }
    }
    private void setDefaultProperties() {
        properties.setProperty("stop.time", "5");
        properties.setProperty("activemq.brokerUrl", "tcp://0.0.0.0:61616");
        properties.setProperty("activemq.userName", "admin");
        properties.setProperty("activemq.password", "admin");
        properties.setProperty("activemq.queue.name", "test");

        logger.warn("Set default properties.");
    }
}
