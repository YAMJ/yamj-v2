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
package com.moviejukebox.model;

import java.util.EnumMap;
import java.util.TimeZone;
import org.apache.commons.lang.WordUtils;
import org.pojava.datetime2.DateTime;

/**
 * Class to store any statistics about the jukebox
 *
 * @author stuart.boston
 */
public class JukeboxStatistics {

    private static final EnumMap<JukeboxStatistic, Integer> statistics = new EnumMap<JukeboxStatistic, Integer>(JukeboxStatistic.class);
    private static DateTime processTime;

    static {
        // Initialise the values
        for (JukeboxStatistic stat : JukeboxStatistic.values()) {
            statistics.put(stat, 0);
        }
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
    public synchronized static void setStatistic(JukeboxStatistic stat, Integer value) {
        statistics.put(stat, value);
    }

    /**
     * Increment the statistic by 1
     *
     * @param stat
     */
    public synchronized static void increment(JukeboxStatistic stat) {
        increment(stat, 1);
    }

    /**
     * Increment the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public synchronized static void increment(JukeboxStatistic stat, Integer amount) {
        Integer current = statistics.get(stat);
        statistics.put(stat, current + amount);
    }

    /**
     * Decrement the statistic by 1
     *
     * @param stat
     */
    public synchronized static void decrement(JukeboxStatistic stat) {
        decrement(stat, 1);
    }

    /**
     * Decrement the statistic by the value
     *
     * @param stat
     * @param amount
     */
    public synchronized static void decrement(JukeboxStatistic stat, Integer amount) {
        Integer current = statistics.get(stat);
        statistics.put(stat, current - amount);
    }

    /**
     * Set the long value for how long processing took
     *
     * @param newProcessTime
     */
    public synchronized static void setProcessingTime(long newProcessTime) {
        processTime = new DateTime(newProcessTime);
    }

    /**
     * Output the jukebox statistics
     *
     * @param skipZero Skip zero values from the output
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
        if (processTime != null) {
            statOutput.append("Processing Time = ").append(processTime.toString("HH:mm:ss.S", TimeZone.getTimeZone("GMT")));
        }

        return statOutput.toString();
    }
}
