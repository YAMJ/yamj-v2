/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import org.apache.log4j.Logger;
import static com.moviejukebox.tools.StringTools.*;

public class SystemTools {
    private static final Logger logger = Logger.getLogger("moviejukebox");
    private static final boolean showMemory = PropertiesUtil.getBooleanProperty("mjb.showMemory", "false");
    private static final float cacheOff = (float) PropertiesUtil.getIntProperty("mjb.cacheOffPercentage", "25");

    public static class Base64 {
        public static String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";

        public static int splitLinesAt = 76;

        public static String base64Encode(String string) {
            String unEncoded = string; // Copy the string so we can modify it
            StringBuffer encoded = new StringBuffer();
            // determine how many padding bytes to add to the output
            int paddingCount = (3 - (unEncoded.length() % 3)) % 3;
            // add any necessary padding to the input
            unEncoded += "\0\0".substring(0, paddingCount);
            // process 3 bytes at a time, churning out 4 output bytes
            // worry about CRLF insertions later
            for (int i = 0; i < unEncoded.length(); i += 3) {
                int j = (unEncoded.charAt(i) << 16) + (unEncoded.charAt(i + 1) << 8) + unEncoded.charAt(i + 2);
                encoded.append(base64code.charAt((j >> 18) & 0x3f) + base64code.charAt((j >> 12) & 0x3f) + base64code.charAt((j >> 6) & 0x3f)
                                + base64code.charAt(j & 0x3f));
            }
            // replace encoded padding nulls with "="
            // return encoded;
            return "Basic " + encoded.toString();
        }
    }

    /**
     * Show the memory available to the program and optionally try to force a garbage collection
     */
    public static void showMemory(boolean showAll) {
        if (showMemory) {
            /* This will return Long.MAX_VALUE if there is no preset limit */
            long memoryMaximum   = Runtime.getRuntime().maxMemory();
            long memoryAllocated = Runtime.getRuntime().totalMemory();
            long memoryFree      = Runtime.getRuntime().freeMemory();
            
            float memoryPercentage = (float)(((float)memoryFree/(float)memoryAllocated)*100F);
            
            if (showAll) {
                /* Maximum amount of memory the JVM will attempt to use */
                logger.info("  Maximum memory: " + (memoryMaximum == Long.MAX_VALUE ? "no limit" : formatFileSize(memoryMaximum)));

                /* Total memory currently in use by the JVM */
                logger.info("Allocated memory: " + formatFileSize(memoryAllocated));

                /* Total amount of free memory available to the JVM */
                logger.info("     Free memory: " + formatFileSize(memoryFree)  + " (" + (int)memoryPercentage + "%)");
            } else {
                logger.info("Memory - Maximum: " + formatFileSize(memoryMaximum) + ", Allocated: " + formatFileSize(memoryAllocated) + ", Free: "
                                + formatFileSize(memoryFree) + " (" + (int)memoryPercentage + "%)");
            }

            if (memoryPercentage < cacheOff) {
                Cache.purgeCache();
            }
            
        }
        // Run garbage collection (if needed)
        System.gc();
    }

    public static void showMemory() {
        showMemory(false);
    }

}