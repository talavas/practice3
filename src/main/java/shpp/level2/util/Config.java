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

        loadPropertiesFromClasspath(PROPERTIES_FILE_NAME);

        if(properties.isEmpty()) {
            setDefaultProperties();
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
    private void loadPropertiesFromClasspath(String filePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath)
        ) {
            if (inputStream != null) {
                logger.debug("Load properties from '{}' file.", filePath);
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }else{
                throw new IOException(filePath);
            }
        } catch (IOException e) {
            logger.debug("Can't get properties from file {}.", filePath, e);
            logger.warn("Can't load properties file '{}'", filePath);
        }
    }
    private void setDefaultProperties() {
        properties.setProperty("min", "1");
        properties.setProperty("max", "10");
        properties.setProperty("inc", "1");

        logger.warn("Set default properties min={}, max={}, inc={}",
                properties.getProperty("min"),
                properties.getProperty("max"),
                properties.getProperty("inc"));
    }
}
