/*
 *      Copyright (c) 2004-2012 YAMJ Members
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

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.log4j.Logger;

/**
 *
 * @author altman.matthew
 */
public class PropertiesUtil {

    private static final Logger logger = Logger.getLogger(PropertiesUtil.class);
    private static final String logMessage = "PropertiesUtil: ";
    private static final String PROPERTIES_CHARSET = "UTF-8";
    private static Properties props = new Properties();
    private static final String PREFERENCES_FILENAME = "preferences.xsl";

    public static boolean setPropertiesStreamName(String streamName) {
        return setPropertiesStreamName(streamName, true);
    }

    public static boolean setPropertiesStreamName(String streamName, boolean warnFatal) {
        logger.info("Using properties file " + streamName);
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(streamName);
        Reader reader = null;

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream(streamName);
            }

            reader = new InputStreamReader(propertiesStream, PROPERTIES_CHARSET);
            props.load(reader);
        } catch (IOException error) {
            // Output a warning if required.
            if (warnFatal) {
                logger.error("Failed loading file " + streamName + ": Please check your configuration. The properties file should be in the classpath.");
            } else {
                logger.warn("Warning (non-fatal): User properties file '" + streamName + "', not found.");
            }
            return false;
        } finally {
            try {
                if (propertiesStream != null) {
                    propertiesStream.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return true;
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Return the key property as an integer
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getIntProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();
        try {
            return Integer.parseInt(property);
        } catch (NumberFormatException nfe) {
            return Integer.parseInt(defaultValue);
        }
    }

    /**
     * Return the key property as an long
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLongProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();
        try {
            return Long.parseLong(property);
        } catch (NumberFormatException nfe) {
            return Long.parseLong(defaultValue);
        }
    }

    /**
     * Return the key property as a boolean
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();
        try {
            return Boolean.parseBoolean(property);
        } catch (NumberFormatException nfe) {
            return Boolean.parseBoolean(defaultValue);
        }
    }

    /**
     * Return the key property as a float
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloatProperty(String key, String defaultValue) {
        String property = getProperty(key, defaultValue).trim();

        try {
            return Float.parseFloat(property);
        } catch (NumberFormatException nfe) {
            return Float.parseFloat(defaultValue);
        }
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
    @SuppressWarnings("serial")
    public static class KeywordMap extends HashMap<String, String> {

        private final List<String> keywords = new ArrayList<String>();

        public List<String> getKeywords() {
            return keywords;
        }
    }

    /**
     * Collect keywords list and appropriate keyword values. Example: my.languages = EN,FR my.languages.EN = English
     * my.languages.FR = French
     *
     * @param prefix Key for keywords list and prefix for value searching.
     * @return Ordered keyword list and map.
     */
    public static KeywordMap getKeywordMap(String prefix, String defaultValue) {
        KeywordMap keywordMap = new KeywordMap();

        String languages = getProperty(prefix, defaultValue);
        if (!isBlank(languages)) {
            for (String lang : languages.split("[ ,]+")) {
                lang = StringUtils.trimToNull(lang);
                if (lang == null) {
                    continue;
                }
                keywordMap.keywords.add(lang);
                String values = getProperty(prefix + "." + lang);
                if (values != null) {
                    keywordMap.put(lang, values);
                }
            }
        }

        return keywordMap;
    }

    public static void writeProperties() {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        Writer out = null;

        // Save the properties in order
        List<String> propertiesList = new ArrayList<String>();
        for (Object propertyObject : props.keySet()) {
            propertiesList.add((String) propertyObject);
        }
        // Sort the properties
        Collections.sort(propertiesList);

        try {
            logger.debug(logMessage + "Writing skin preferences file to " + getPropertiesFilename(true));

            fos = new FileOutputStream(getPropertiesFilename(true));
            osw = new OutputStreamWriter(fos, "UTF-8");
            out = new BufferedWriter(osw);

            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            out.write("<!-- This file is written automatically by YAMJ -->\n");
            out.write("<!-- Last updated: " + (new Date()) + " -->\n");

            out.write("<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n");
            out.write("    <xsl:output method=\"xml\" omit-xml-declaration=\"yes\" />\n");

            for (String property : propertiesList) {
                if (!property.startsWith("API_KEY") && !property.contains("#")) {
                    out.write("    <xsl:param name=\"" + property + "\" />\n");
                }
            }

            out.write("</xsl:stylesheet>\n");
            out.flush();
        } catch (IOException error) {
            // Can't write to file
            logger.error(logMessage + "Can't write to file");
            logger.error(SystemTools.getStackTrace(error));
        } catch (Exception error) {
            // Some other error
            logger.error(logMessage + "Error with file");
            logger.error(SystemTools.getStackTrace(error));
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (Exception error) {
                    // ignore this error
                }
            }

            if (osw != null) {
                try {
                    osw.flush();
                    osw.close();
                } catch (Exception error) {
                    // ignore this error
                }
            }

            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (Exception error) {
                    // ignore this error
                }
            }
        }
    }

    public static String getPropertiesFilename(boolean fullPath) {
        if (fullPath) {
            return StringTools.appendToPath(getProperty("mjb.skin.dir", "./skins/default"), PREFERENCES_FILENAME);
        } else {
            return PREFERENCES_FILENAME;
        }
    }
}
