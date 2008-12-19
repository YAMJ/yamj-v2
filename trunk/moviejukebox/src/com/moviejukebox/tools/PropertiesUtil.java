package com.moviejukebox.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * 
 * @author altman.matthew
 */
public class PropertiesUtil {

    private static Logger logger = Logger.getLogger("moviejukebox");

    private static Properties props = null;

    public static boolean setPropertiesStreamName(String streamName) {
        logger.fine("Using properties file " + streamName);
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(streamName);

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream(streamName);
            }

            props = new Properties();
            props.load(propertiesStream);
        } catch (IOException e) {
            logger.severe("Failed loading file " + streamName + ": Please check your configuration. The moviejukebox.properties should be in the classpath.");
            return false;
        }

        logger.finer(props.toString());

        String skinHome = props.getProperty(
            "mjb.skin.dir", "./skins/default");

        File skinProperties = new File(skinHome, "skin.properties");
        try {
            propertiesStream = new FileInputStream(skinProperties);
            props.load(propertiesStream);
        } catch (Exception e) {
            logger
                .severe("Failed loading file "
                                + skinProperties.getAbsolutePath()
                                + ": Please check your configuration. The moviejukebox.properties should be in the classpath, and define a property called mjb.skin.dir which point to the skin directory.");
            return false;
        }
        return true;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(
            key, defaultValue);
    }

    // Issue 309
    public static Set<Entry<Object, Object>> getEntrySet() {
        return props.entrySet();
    }
}
