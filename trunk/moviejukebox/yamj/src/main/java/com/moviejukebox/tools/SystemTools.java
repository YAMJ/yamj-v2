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

import com.moviejukebox.MovieJukebox;
import static com.moviejukebox.tools.StringTools.formatFileSize;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class SystemTools {

    private static final Logger logger = Logger.getLogger(SystemTools.class);
    private static final boolean showMemory = PropertiesUtil.getBooleanProperty("mjb.showMemory", "false");
    private static final long cacheOff = (long) (PropertiesUtil.getIntProperty("mjb.cacheOffSize", "50") * 1024 * 1024);

    /**
     * Show the memory available to the program and optionally try to force a
     * garbage collection
     */
    public static void showMemory(boolean showAll) {

        /*
         * This will return Long.MAX_VALUE if there is no preset limit
         */
        long memoryMaximum = Runtime.getRuntime().maxMemory();
        long memoryAllocated = Runtime.getRuntime().totalMemory();
        long memoryFree = Runtime.getRuntime().freeMemory();
        float memoryPercentage = (float) (((float) memoryFree / (float) memoryMaximum) * 100F);

        if (showMemory) {
            if (showAll) {
                /*
                 * Maximum amount of memory the JVM will attempt to use
                 */
                logger.info("  Maximum memory: " + (memoryMaximum == Long.MAX_VALUE ? "no limit" : formatFileSize(memoryMaximum)));

                /*
                 * Total memory currently in use by the JVM
                 */
                logger.info("Allocated memory: " + formatFileSize(memoryAllocated));

                /*
                 * Total amount of free memory available to the JVM
                 */
                logger.info("     Free memory: " + formatFileSize(memoryFree) + " (" + (int) memoryPercentage + "%)");
            } else {
                logger.info("Memory - Maximum: " + formatFileSize(memoryMaximum) + ", Allocated: " + formatFileSize(memoryAllocated) + ", Free: "
                        + formatFileSize(memoryFree) + " (" + (int) memoryPercentage + "%)");
            }
        }

        // Check to see if we need to turn the cache off.
        if (memoryFree < cacheOff) {
            CacheMemory.purgeCache();
        }

        // Run garbage collection (if needed)
        System.gc();
    }

    /**
     * Show the memory available to the program and optionally try to force a
     * garbage collection
     */
    public static void showMemory() {
        showMemory(false);
    }

    /**
     * Helper method that throws an exception and saves it to the log as well.
     *
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
     *
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

    /**
     * Check the 'lib' directory to see if any of the common API jars have
     * duplicates and warn if they do
     *
     * @return
     */
    public static boolean validateInstallation() {
        boolean installationIsValid = Boolean.TRUE;

        if (PropertiesUtil.getBooleanProperty("mjb.skipCheckJars", "false")) {
            return installationIsValid;
        }

        // List of the jars to check for duplicates
        HashMap<String, List<String>> jarsToCheck = new HashMap<String, List<String>>();
        jarsToCheck.put("allocine-api", new ArrayList<String>());
        jarsToCheck.put("apirottentomatoes", new ArrayList<String>());
        jarsToCheck.put("fanarttvapi", new ArrayList<String>());
        jarsToCheck.put("mjbsqldb", new ArrayList<String>());
        jarsToCheck.put("themoviedbapi", new ArrayList<String>());
        jarsToCheck.put("thetvdbapi", new ArrayList<String>());
        jarsToCheck.put("tvrageapi", new ArrayList<String>());

        List<String> jarList = Arrays.asList(System.getProperty("java.class.path").split(";"));
        for (String currentJar : jarList) {
            // Only check the lib directory
            if (currentJar.startsWith("lib/")) {
                currentJar = currentJar.substring(4);
            } else {
                continue;
            }

            for (Map.Entry<String, List<String>> entry : jarsToCheck.entrySet()) {
                if (currentJar.contains(entry.getKey())) {
                    entry.getValue().add(currentJar);
                    if (entry.getValue().size() > 1) {
                        installationIsValid = Boolean.FALSE;
                    }
                    // No need to check for further matches
                    continue;
                }
            }
        }

        if (!installationIsValid) {
            logger.error("WARNING: Your installation appears to be invalid.");
            logger.error("WARNING: Please ensure you delete the 'lib' folder before you update!");
            logger.error("WARNING: You will need to re-install YAMJ now to ensure correct running!");
            logger.error("");
            logger.error("The following duplicates were found:");

            for (Map.Entry<String, List<String>> entry : jarsToCheck.entrySet()) {
                if (entry.getValue().size() > 1) {
                    logger.error(entry.getKey() + ":");
                    for (String dupJar : entry.getValue()) {
                        logger.error("    - " + dupJar);
                    }
                }
            }
        }

        return installationIsValid;
    }

    /**
     * Get the currently running YAMJ revision
     *
     * @return
     */
    public static String getRevision() {
        // Jukebox revision
        String currentRevision = MovieJukebox.class.getPackage().getImplementationVersion();
        // If YAMJ is self compiled then the revision information may not exist.
        if (StringUtils.isBlank(currentRevision) || (currentRevision.equalsIgnoreCase("${env.SVN_REVISION}"))) {
            currentRevision = "0000";
        }
        return currentRevision;
    }

    /**
     * Get the currently running YAMJ version
     *
     * @return
     */
    public static String getVersion() {
        String mjbVersion = MovieJukebox.class.getPackage().getSpecificationVersion();
        if (StringUtils.isBlank(mjbVersion)) {
            return "";
        } else {
            return mjbVersion;
        }
    }

    /**
     * Get the build date of YAMJ
     *
     * @return
     */
    public static String getBuildDate() {
        return MovieJukebox.class.getPackage().getImplementationTitle();
    }
}