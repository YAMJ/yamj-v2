/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

/**
 * This class is used to limit the movies updated for each run of YAMJ.
 *
 * getToken must be called before checking a movie. On return true, the movie
 * can be checked. If movie is not updated, the token must be released with
 * releaseToken.
 *
 * @author iuk
 */
public class ScanningLimit {

    private static final Logger LOGGER = Logger.getLogger(ScanningLimit.class);
    private static final String LOG_MESSAGE = "ScanningLimit: ";
    private static final int CHECK_MAX = PropertiesUtil.getIntProperty("mjb.check.Max", "0");
    private static int tokensUsed = 0;
    private static boolean limitReached = Boolean.FALSE;

    /**
     * Take a token from the pool
     *
     * @return TRUE if a token was assigned
     */
    public static synchronized boolean getToken() {
        if (CHECK_MAX <= 0) {
            // No limit imposed
            return true;
        }

        if (tokensUsed < CHECK_MAX) {
            tokensUsed++;
            LOGGER.trace(LOG_MESSAGE + "Got token (" + (CHECK_MAX - tokensUsed) + " left)");
        } else {
            LOGGER.debug(LOG_MESSAGE + "Maximum scan limit of " + CHECK_MAX + " reached");
            limitReached = Boolean.TRUE;
        }
        // Return value: Was the token assigned
        return (!limitReached);
    }

    /**
     * Get the limit for scanning
     *
     * @return
     */
    public static int getLimit() {
        return CHECK_MAX;
    }

    /**
     * Release a token back into the pool
     */
    public static synchronized void releaseToken() {
        if (CHECK_MAX > 0) {
            tokensUsed--;
            LOGGER.trace(LOG_MESSAGE + "Released token (" + (CHECK_MAX - tokensUsed) + " left)");
        }
    }

    /**
     * Check to see if the limit has been reached for the tokens.
     *
     * @return
     */
    public static boolean isLimitReached() {
        return limitReached;
    }
}