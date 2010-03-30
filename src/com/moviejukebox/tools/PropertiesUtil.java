/*
 *      Copyright (c) 2004-2010 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */

package com.moviejukebox.tools;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import com.moviejukebox.scanner.MovieFilenameScanner;

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
            propertiesStream.close();
            reader.close();
        } catch (IOException error) {
            // Output a warning if moviejukebox.properties isn't found. Otherwise it's an error
            if (streamName.contains("moviejukebox.properties")) {
                logger.warning("Warning (non-fatal): User properties file: moviejukebox.properties, not found.");
            } else {
                logger.severe("Failed loading file " + streamName + ": Please check your configuration. The properties file should be in the classpath.");
            }
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
    
    /**
     * Store list (ordered) and keyword map.
     */
    public static class KeywordMap extends HashMap<String, String> {
        private final List<String> keywords = new ArrayList<String>();
        
        public List<String> getKeywords() {
            return keywords;
        }
    }
    
    /**
     * Collect keywords list and appropriate keyword values.<br>
     * Example:<br>
     * my.languages = EN,FR<br>
     * my.languages.EN = English<br>
     * my.languages.FR = French<br>
     * 
     * @param prefix Key for keywords list and prefix for value searching.
     * @return Ordered keyword list and map.
     */
    public static KeywordMap getKeywordMap(String prefix, String defaultValue) {
        KeywordMap m = new KeywordMap();

        String languages = getProperty(prefix, defaultValue);
        if (!isBlank(languages)) {
            for (String lang : languages.split("[ ,]+")) {
                lang = StringUtils.trimToNull(lang);
                if (lang == null) {
                    continue;
                }
                m.keywords.add(lang);
                String values = getProperty(prefix + lang);
                if (values != null) {
                    m.put(lang, values);
                }
            }
        }
        
        return m;
    }
    
}
