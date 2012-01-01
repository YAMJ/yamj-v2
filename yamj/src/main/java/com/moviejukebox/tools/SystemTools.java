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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.log4j.Logger;
import static com.moviejukebox.tools.StringTools.*;

public class SystemTools {

    private static final Logger logger = Logger.getLogger("moviejukebox");
    private static final boolean showMemory = PropertiesUtil.getBooleanProperty("mjb.showMemory", "false");
    private static final long cacheOff = (long) (PropertiesUtil.getIntProperty("mjb.cacheOffSize", "50") * 1024 * 1024);

    /**
     * Show the memory available to the program and optionally try to force a garbage collection
     */
    public static void showMemory(boolean showAll) {

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long memoryMaximum = Runtime.getRuntime().maxMemory();
        long memoryAllocated = Runtime.getRuntime().totalMemory();
        long memoryFree = Runtime.getRuntime().freeMemory();
        float memoryPercentage = (float) (((float) memoryFree / (float) memoryMaximum) * 100F);

        if (showMemory) {
            if (showAll) {
                /* Maximum amount of memory the JVM will attempt to use */
                logger.info("  Maximum memory: " + (memoryMaximum == Long.MAX_VALUE ? "no limit" : formatFileSize(memoryMaximum)));

                /* Total memory currently in use by the JVM */
                logger.info("Allocated memory: " + formatFileSize(memoryAllocated));

                /* Total amount of free memory available to the JVM */
                logger.info("     Free memory: " + formatFileSize(memoryFree) + " (" + (int) memoryPercentage + "%)");
            } else {
                logger.info("Memory - Maximum: " + formatFileSize(memoryMaximum) + ", Allocated: " + formatFileSize(memoryAllocated) + ", Free: "
                        + formatFileSize(memoryFree) + " (" + (int) memoryPercentage + "%)");
            }
        }

        // Check to see if we need to turn the cache off.
        if (memoryFree < cacheOff) {
            Cache.purgeCache();
        }

        // Run garbage collection (if needed)
        System.gc();
    }

    /**
     * Show the memory available to the program and optionally try to force a garbage collection
     */
    public static void showMemory() {
        showMemory(false);
    }

    /**
     * Helper method that throws an exception and saves it to the log as well.
     * @param text
     */
    public static void logException(String text) {
        logger.error("***** GENERATED EXCEPTION *****");
        Exception tw = new Exception(text);
        final Writer sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        tw.printStackTrace(pw);
        logger.error(sw.toString());
    }

    /**
     * Helper method to print the stack trace to the log file
     * @param tw
     * @return
     */
    public static String getStackTrace(Throwable tw) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        tw.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

}