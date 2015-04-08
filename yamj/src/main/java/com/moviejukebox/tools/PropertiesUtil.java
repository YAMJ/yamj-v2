/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.tools;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties processing class for YAMJ
 *
 * @author altman.matthew
 */
public final class PropertiesUtil {

    private static final Logger LOG = LoggerFactory.getLogger(PropertiesUtil.class);
    private static final String PROPERTIES_CHARSET = "UTF-8";
    private static final String PREFERENCES_FILENAME = "preferences.xsl";

    /**
     * String representing TRUE
     */
    public static final String TRUE = "true";

    /**
     * String representing FALSE
     */
    public static final String FALSE = "false";
    private static final Properties PROPS = new Properties();

    private PropertiesUtil() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Set the properties filename
     *
     * @param streamName
     * @return
     */
    public static boolean setPropertiesStreamName(String streamName) {
        return setPropertiesStreamName(streamName, Boolean.TRUE);
    }

    /**
     * Set the properties filename with a warning if the file is not found
     *
     * @param streamName
     * @param warnFatal
     * @return
     */
    public static boolean setPropertiesStreamName(final String streamName, boolean warnFatal) {
        LOG.info("Using properties file '{}'", FilenameUtils.normalize(streamName));
        InputStream propertiesStream = ClassLoader.getSystemResourceAsStream(streamName);
        Reader reader = null;

        try {
            if (propertiesStream == null) {
                propertiesStream = new FileInputStream(streamName);
            }

            reader = new InputStreamReader(propertiesStream, PROPERTIES_CHARSET);
            PROPS.load(reader);
        } catch (IOException error) {
            // Output a warning if required.
            if (warnFatal) {
                LOG.error("Failed loading file {}: Please check your configuration. The properties file should be in the classpath.", streamName, error);
            } else {
                LOG.debug("Warning (non-fatal): User properties file '{}', not found.", streamName);
            }
            return Boolean.FALSE;
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
        return Boolean.TRUE;
    }

    /**
     * Get a property via a key.
     *
     * @param key
     * @return the value if found, otherwise null
     */
    public static String getProperty(String key) {
        return PROPS.getProperty(key);
    }

    /**
     * Get a property via a key
     *
     * @param key
     * @param defaultValue
     * @return the value if found, otherwise the default value
     */
    public static String getProperty(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    /**
     * Return the key property as a boolean
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        return convertBooleanProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as integer
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static int getIntProperty(String key, int defaultValue) {
        return convertIntegerProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as an long
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static long getLongProperty(String key, long defaultValue) {
        return convertLongProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Return the key property as a float
     *
     * @param key
     * @param defaultValue
     * @return
     */
    public static float getFloatProperty(String key, float defaultValue) {
        return convertFloatProperty(PROPS.getProperty(key), defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static String getReplacedProperty(String newKey, String oldKey, String defaultValue) {
        return getReplacedKeyValue(newKey, oldKey);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static boolean getReplacedBooleanProperty(String newKey, String oldKey, boolean defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertBooleanProperty(property, defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static int getReplacedIntProperty(String newKey, String oldKey, int defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertIntegerProperty(property, defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static long getReplacedLongProperty(String newKey, String oldKey, long defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertLongProperty(property, defaultValue);
    }

    /**
     * Returns the property specified by the newKey or oldKey.
     *
     * Outputs warning if the oldKey was found.
     *
     * @param newKey
     * @param oldKey
     * @param defaultValue
     * @return
     */
    public static float getReplacedFloatProperty(String newKey, String oldKey, float defaultValue) {
        String property = getReplacedKeyValue(newKey, oldKey);
        return convertFloatProperty(property, defaultValue);
    }

    /**
     * Look for both keys in the property list and warn if the old one was found
     *
     * @param newKey
     * @param oldKey
     * @return
     */
    private static String getReplacedKeyValue(String newKey, String oldKey) {
        String oldProperty = StringUtils.trimToNull(PROPS.getProperty(oldKey));
        String newProperty = StringUtils.trimToNull(PROPS.getProperty(newKey));
        String returnValue;

        if (newProperty == null && oldProperty != null) {
            // We found the old property, but not the new
            LOG.warn("Property '{}' has been deprecated and will be removed; please use '{}' instead.", oldKey, newKey);
            returnValue = oldProperty;
        } else if (newProperty != null && oldProperty != null) {
            // We found both properties, so warn about the old one
            LOG.warn("Property '{}' is no longer used, but was found in your configuration files, please remove it.", oldKey);
            returnValue = newProperty;
        } else {
            returnValue = newProperty;
        }

        return returnValue;
    }

    /**
     * Convert the value to a Float
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static float convertFloatProperty(String valueToConvert, float defaultValue) {
        return NumberUtils.toFloat(StringUtils.trimToEmpty(valueToConvert), defaultValue);
    }

    /**
     * Convert the value to a Long
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static long convertLongProperty(String valueToConvert, long defaultValue) {
        return NumberUtils.toLong(StringUtils.trimToEmpty(valueToConvert), defaultValue);
    }

    /**
     * Convert the value to a Integer
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static int convertIntegerProperty(String valueToConvert, int defaultValue) {
        return NumberUtils.toInt(StringUtils.trimToEmpty(valueToConvert), defaultValue);
    }

    /**
     * Convert the value to a Boolean
     *
     * @param key
     * @param valueToConvert
     * @param defaultValue
     * @return
     */
    private static boolean convertBooleanProperty(String valueToConvert, boolean defaultValue) {
        boolean value = defaultValue;
        if (StringUtils.isNotBlank(valueToConvert)) {
            value = Boolean.parseBoolean(StringUtils.trimToEmpty(valueToConvert));
        }
        return value;
    }

    /**
     * Get the properties as an entry set for iteration<br>
     * Issue 309
     *
     * @return
     */
    public static Set<Entry<Object, Object>> getEntrySet() {
        // Issue 728
        // Shamelessly adapted from: http://stackoverflow.com/questions/54295/how-to-write-java-util-properties-to-xml-with-sorted-keys
        return new TreeMap<>(PROPS).entrySet();
    }

    /**
     * Set a property key to the string value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, String value) {
        PROPS.setProperty(key, value);
    }

    /**
     * Set a property key to the boolean value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, boolean value) {
        PROPS.setProperty(key, Boolean.toString(value));
    }

    /**
     * Set a property key to the integer value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, int value) {
        PROPS.setProperty(key, Integer.toString(value));
    }

    /**
     * Set a property key to the long value
     *
     * @param key
     * @param value
     */
    public static void setProperty(String key, long value) {
        PROPS.setProperty(key, Long.toString(value));
    }

    /**
     * Store list (ordered) and keyword map.
     */
    public static class KeywordMap extends HashMap<String, String> {

        private static final long serialVersionUID = 1L;
        private final transient List<String> keywords = new ArrayList<>();

        /**
         * Get the list of keywords
         *
         * @return
         */
        public List<String> getKeywords() {
            return keywords;
        }
    }

    /**
     * Collect keywords list and appropriate keyword values. <br>
     * Example: my.languages = EN,FR my.languages.EN = English my.languages.FR =
     * French
     *
     * @param prefix Key for keywords list and prefix for value searching.
     * @param defaultValue
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

    /**
     * Output a warning message about the property being no longer used
     *
     * @param prop Property to warn about
     */
    public static void warnDeprecatedProperty(final String prop) {
        String value = StringUtils.trimToNull(PROPS.getProperty(prop));
        if (StringTools.isValidString(value)) {
            LOG.warn("Property '{}' is no longer used, but was found in your configuration files, please remove it.", prop);
        }
    }

    /**
     * Write the properties out to a file
     */
    public static void writeProperties() {
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        Writer out = null;

        // Save the properties in order
        List<String> propertiesList = new ArrayList<>();
        for (Object propertyObject : PROPS.keySet()) {
            propertiesList.add((String) propertyObject);
        }
        // Sort the properties
        Collections.sort(propertiesList);

        try {
            LOG.debug("Writing skin preferences file to {}", getPropertiesFilename(Boolean.TRUE));

            fos = new FileOutputStream(getPropertiesFilename(Boolean.TRUE));
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
            LOG.error("Can't write to file");
            LOG.error(SystemTools.getStackTrace(error));
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException error) {
                    // ignore this error
                }
            }

            if (osw != null) {
                try {
                    osw.flush();
                    osw.close();
                } catch (IOException error) {
                    // ignore this error
                }
            }

            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException error) {
                    // ignore this error
                }
            }
        }
    }

    /**
     * Get the skin properties filename
     *
     * @param fullPath
     * @return
     */
    public static String getPropertiesFilename(boolean fullPath) {
        if (fullPath) {
            return StringTools.appendToPath(getProperty("mjb.skin.dir", "./skins/default"), PREFERENCES_FILENAME);
        } else {
            return PREFERENCES_FILENAME;
        }
    }
}
