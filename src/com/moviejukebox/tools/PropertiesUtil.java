package com.moviejukebox.tools;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * 
 * @author altman.matthew
 */
public class PropertiesUtil {

    private static final String PROPERTIES_CHARSET = "UTF-8";
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static Properties props = new Properties();

    public static boolean setPropertiesStreamName(String streamName) {
        logger.fine("Using properties file " + streamName);
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(streamName);

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream(streamName);
            }

            Reader reader = new InputStreamReader(propertiesStream, PROPERTIES_CHARSET);
            props.load(reader);

        } catch (IOException e) {
            logger.severe("Failed loading file " + streamName + ": Please check your configuration. The moviejukebox.properties should be in the classpath.");
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
        // Issue 728
        // Shamelessly adapted from: http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys
        return new TreeMap<Object, Object>(props).entrySet();
    }
    
    public static void setProperty(String key, String value) {
        props.setProperty(key, value);
    }
}
