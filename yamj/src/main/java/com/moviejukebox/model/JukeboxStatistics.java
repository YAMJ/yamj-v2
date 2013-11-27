/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.model;

import com.moviejukebox.tools.DOMHelper;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.pojava.datetime2.DateTime;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class to store any statistics about the jukebox
 *
 * @author stuart.boston
 */
public final class JukeboxStatistics {

    // Logger
    private static final Logger LOG = Logger.getLogger(JukeboxStatistics.class);
    private static final String LOG_MESSAGE = "JukeboxStatistics: ";
    // Filename
    private static final String XML_FILENAME = "jukebox_statistics.xml";
    // Properties
    private static final EnumMap<JukeboxStatistic, Integer> statistics = new EnumMap<JukeboxStatistic, Integer>(JukeboxStatistic.class);
    private static final EnumMap<JukeboxTimes, Long> TIMES = new EnumMap<JukeboxTimes, Long>(JukeboxTimes.class);
    // Literals
    private static final String DEFAULT_FORMAT = "HH:mm:ss.S";
    private static final String DEFAULT_TZ = "GMT";

    static {
        // Add the file to the list of safe files
        FileTools.addJukeboxFile(XML_FILENAME);
        // Initialise the values
        for (JukeboxStatistic stat : JukeboxStatistic.values()) {
            statistics.put(stat, 0);
        }
    }

    /**
     * List of times for the jukebox processing
     */
    public enum JukeboxTimes {

        START,
        SCAN_END,
        PROCESSING_END,
        PEOPLE_END,
        INDEXING_END,
        MASTERS_END,
        WRITE_INDEX_END,
        WRITE_PEOPLE_END,
        WRITE_HTML_END,
        COPYING_END,
        END;
    }

    private JukeboxStatistics() {
        throw new IllegalArgumentException("Class cannot be instantiated");
    }

    /**
     * Get the current value of the required statistic
     *
     * @param stat
     * @return
     */
    public static int getStatistic(JukeboxStatistic stat) {
        return statistics.get(stat);
    }

    /**
     * Set the statistic to a specific value
     *
     * @param stat
     * @param value
     */
    public static synchronized void setStatistic(JukeboxStatistic stat, Integer value) {
        statistics.put(stat, value);
    }

    /**
     * Increment the statistic by 1
     *
     * @param stat
     */
    public static synchronized void increment(JukeboxStatistic stat) {
        increment(stat, 1);
    }

    /**
     * Increment the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public static synchronized void increment(JukeboxStatistic stat, Integer amount) {
        Integer current = statistics.get(stat);
        statistics.put(stat, current + amount);
    }

    /**
     * Decrement the statistic by 1
     *
     * @param stat
     */
    public static synchronized void decrement(JukeboxStatistic stat) {
        decrement(stat, 1);
    }

    /**
     * Decrement the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public static synchronized void decrement(JukeboxStatistic stat, Integer amount) {
        Integer current = statistics.get(stat);
        statistics.put(stat, current - amount);
    }

    /**
     * Set the start time of the jukebox processing
     *
     * @param timeValue
     */
    public static void setTimeStart(long timeValue) {
        setJukeboxTime(JukeboxTimes.START, timeValue);
    }

    /**
     * Set the end time of the jukebox processing
     *
     * @param timeValue
     */
    public static void setTimeEnd(long timeValue) {
        setJukeboxTime(JukeboxTimes.END, timeValue);
    }

    /**
     * Set a time for the jukebox processing
     *
     * @param timeType
     * @param timeValue
     */
    public static void setJukeboxTime(JukeboxTimes timeType, long timeValue) {
        JukeboxStatistics.TIMES.put(timeType, timeValue);
    }

    /**
     * Calculate the difference between two jukebox times
     *
     * @param timeStart
     * @param timeEnd
     * @return
     */
    public static String getProcessingTime(JukeboxTimes timeStart, JukeboxTimes timeEnd) {
        if (TIMES.containsKey(timeStart) && TIMES.containsKey(timeEnd)) {
            DateTime processTime = new DateTime(TIMES.get(JukeboxTimes.END) - TIMES.get(JukeboxTimes.START));
            return processTime.toString(DEFAULT_FORMAT, TimeZone.getTimeZone(DEFAULT_TZ));
        } else {
            return "";
        }
    }

    /**
     * Calculate the processing time for the jukebox run. Uses the the START and END times.
     *
     * @return
     */
    public static String getProcessingTime() {
        return getProcessingTime(JukeboxTimes.START, JukeboxTimes.END);
    }

    /**
     * Get a formatted string of the time type
     *
     * @param timeType
     * @param timeFormat
     * @return
     */
    public static String getTime(JukeboxTimes timeType, String timeFormat) {
        String returnValue = "";
        if (TIMES.containsKey(timeType)) {
            if (StringTools.isValidString(timeFormat)) {
                returnValue = (new DateTime(TIMES.get(timeType)).toString(timeFormat));
            } else {
                returnValue = (new DateTime(TIMES.get(timeType)).toString(DEFAULT_FORMAT));

            }
        }
        return returnValue;
    }

    /**
     * Get the (long) time of the time type
     *
     * @param timeType
     * @return
     */
    public static long getTime(JukeboxTimes timeType) {
        if (TIMES.containsKey(timeType)) {
            return TIMES.get(timeType);
        } else {
            return 0;
        }
    }

    /**
     * Output the jukebox statistics
     *
     * @param skipZero Skip zero values from the output
     * @return
     */
    public static String generateStatistics(Boolean skipZero) {
        StringBuilder statOutput = new StringBuilder("Jukebox Statistics:\n");

        // Build the counts
        int value;
        for (JukeboxStatistic stat : JukeboxStatistic.values()) {
            value = statistics.get(stat);
            if (value > 0 || !skipZero) {
                statOutput.append(WordUtils.capitalizeFully(stat.toString().replace("_", " ").toLowerCase()));
                statOutput.append(" = ").append(value).append("\n");
            }
        }

        // Add the processing time
        String processTime = getProcessingTime();
        if (StringTools.isValidString(processTime)) {
            statOutput.append("Processing Time = ").append(processTime);
        }

        return statOutput.toString();
    }

    /**
     * Write the statistics to a file
     *
     * @param jukebox
     * @param library
     * @param mediaLibraryPaths
     */
    public static void writeFile(Jukebox jukebox, Library library, Collection<MediaLibraryPath> mediaLibraryPaths) {
        File jbStats = new File(jukebox.getJukeboxRootLocationDetailsFile(), XML_FILENAME);
        FileTools.addJukeboxFile(jbStats.getName());

        Document docJbStats;
        Element eRoot, eStats, eTimes;

        try {
            LOG.debug(LOG_MESSAGE + "Creating JukeboxStatistics file: " + jbStats.getAbsolutePath());
            if (jbStats.exists() && !jbStats.delete()) {
                LOG.error(LOG_MESSAGE + "Failed to delete " + jbStats.getName() + ". Please make sure it's not read only");
                return;
            }
        } catch (Exception error) {
            LOG.error(LOG_MESSAGE + "Failed to create/delete " + jbStats.getName() + ". Please make sure it's not read only");
            return;
        }

        try {
            // Start with a blank document
            docJbStats = DOMHelper.createDocument();
            String tempString = (new DateTime(System.currentTimeMillis())).toString(DateTimeTools.getDateFormatLongString());
            docJbStats.appendChild(docJbStats.createComment("This file was created on: " + tempString));

            //create the root element and add it to the document
            eRoot = docJbStats.createElement("root");
            docJbStats.appendChild(eRoot);

            // Create the statistics node
            eStats = docJbStats.createElement("statistics");
            eRoot.appendChild(eStats);

            for (Map.Entry<JukeboxStatistic, Integer> entry : statistics.entrySet()) {
                DOMHelper.appendChild(docJbStats, eStats, entry.getKey().toString().toLowerCase(), entry.getValue().toString());
            }
            DOMHelper.appendChild(docJbStats, eStats, "libraries", Integer.toString(mediaLibraryPaths.size()));

            // Create the time node
            eTimes = docJbStats.createElement("times");
            eRoot.appendChild(eTimes);

            DateTime dt;
            for (Map.Entry<JukeboxTimes, Long> entry : TIMES.entrySet()) {
                if (entry.getValue() > 0) {
                    dt = new DateTime(entry.getValue());
                    DOMHelper.appendChild(docJbStats, eTimes, entry.getKey().toString().toLowerCase(), dt.toString(DateTimeTools.getDateFormatLongString()));
                }
            }
            DOMHelper.appendChild(docJbStats, eTimes, "processing", getProcessingTime());

            DOMHelper.writeDocumentToFile(docJbStats, jbStats.getAbsolutePath());
        } catch (ParserConfigurationException ex) {
            LOG.error(LOG_MESSAGE + "Error creating " + jbStats.getName() + " file: " + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        } catch (DOMException ex) {
            LOG.error(LOG_MESSAGE + "Error creating " + jbStats.getName() + " file: " + ex.getMessage());
            LOG.error(SystemTools.getStackTrace(ex));
        }

    }
}
