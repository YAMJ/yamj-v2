/*
 *      Copyright (c) 2004-2016 YAMJ Members
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
