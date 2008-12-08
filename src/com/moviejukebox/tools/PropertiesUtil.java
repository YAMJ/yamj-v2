package com.moviejukebox.tools;

import java.io.File;
import java.io.FileInputStream;
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

    private static Properties props = new Properties();

    static {
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream("moviejukebox.properties");

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream("moviejukebox.properties");
            }

            props.load(propertiesStream);
        } catch (Exception e) {
            logger
                            .severe("Failed loading file moviejukebox.properties: Please check your configuration. The moviejukebox.properties should be in the classpath.");
        }

        logger.finer(props.toString());

        String skinHome = props.getProperty("mjb.skin.dir", "./skins/default");

        try {
            propertiesStream = new FileInputStream(skinHome + File.separator + "skin.properties");
            props.load(propertiesStream);
        } catch (Exception e) {
            logger
                            .severe("Failed loading file skin.properties: Please check your configuration. The moviejukebox.properties should be in the classpath, and define a property called mjb.skin.dir which point to the skin directory.");
        }
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    // Issue 309
    public static Set<Entry<Object, Object>> getEntrySet() {
        return props.entrySet();
    }
}
