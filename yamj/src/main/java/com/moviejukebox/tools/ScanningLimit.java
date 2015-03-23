/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: https://github.com/YAMJ/yamj-v2wiki/License
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */
package com.moviejukebox.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to limit the movies updated for each run of YAMJ.
 *
 * getToken must be called before checking a movie. On return true, the movie
 * can be checked. If movie is not updated, the token must be released with
 * releaseToken.
 *
 * @author iuk
 */
public final class ScanningLimit {

    private static final Logger LOG = LoggerFactory.getLogger(ScanningLimit.class);
    private static final int CHECK_MAX = PropertiesUtil.getIntProperty("mjb.check.Max", 0);
    private static int tokensUsed = 0;
    private static boolean limitReached = Boolean.FALSE;

    private ScanningLimit() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

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
            LOG.trace("Got token ({} left)", CHECK_MAX - tokensUsed);
        } else {
            LOG.debug("Maximum scan limit of {} reached", CHECK_MAX);
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
            LOG.trace("Released token ({} left)", CHECK_MAX);
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
