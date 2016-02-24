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
package com.moviejukebox.tools.cache;

import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to provide a caching mechanism for data across threads
 * Initially this will be stored in memory, but ideally should be cached to a
 * database Many sites provide a "last modified" date/time attribute, so we
 * should consider also caching that in the database
 *
 * @author Stuart.Boston
 *
 */
public final class CacheMemory {

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(CacheMemory.class);
    private static boolean cacheEnabled = initCacheState();

    private CacheMemory() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Add an item to the cache. If the item currently exists in the cache it
     * will be removed before being added.
     *
     * @param key
     * @param value
     */
    public static void addToCache(String key, Object value) {
        if (!cacheEnabled) {
            return;
        }

        if (CACHE.containsKey(key)) {
            LOG.debug("Cache (Add): Already contains object ({}) with key '{}' overwriting...", value.getClass().getSimpleName(), key);
            CACHE.remove(key);
        } else {
            LOG.debug("Cache (Add): Adding object ({}) for key '{}'", value.getClass().getSimpleName(), key);
        }
        CACHE.put(key, value);
    }

    /**
     * Get an item from the cache
     *
     * @param key
     * @return
     */
    public static Object getFromCache(String key) {
        if (!cacheEnabled) {
            return null;
        }

        if (CACHE.containsKey(key)) {
            Object value = CACHE.get(key);
            if (value == null) {
                LOG.debug("Cache (Get): No object found for {}", key);
            } else {
                LOG.debug("Cache (Get): Got object ({}) for {}", value.getClass().getSimpleName(), key);
                return value;
            }
        } else {
            LOG.debug("Cache (Get): No object found for {}", key);
        }

        return null;
    }

    /**
     * Delete an item from the cache
     *
     * @param key
     */
    public static void removeFromCache(String key) {
        if (!cacheEnabled) {
            return;
        }

        if (CACHE.containsKey(key)) {
            CACHE.remove(key);
        }
    }

    public static String generateCacheKey(String stringOne, String stringTwo) {
        return generateCacheKey(stringOne, stringTwo, null, null);
    }

    public static String generateCacheKey(String stringOne, String stringTwo, String stringThree) {
        return generateCacheKey(stringOne, stringTwo, stringThree, null);
    }

    public static String generateCacheKey(String stringOne, String stringTwo, String stringThree, String stringFour) {
        List<String> cacheKeys = new ArrayList<>();
        if (StringTools.isValidString(stringOne)) {
            cacheKeys.add(stringOne);
        }

        if (StringTools.isValidString(stringTwo)) {
            cacheKeys.add(stringTwo);
        }

        if (StringTools.isValidString(stringThree)) {
            cacheKeys.add(stringThree);
        }

        if (StringTools.isValidString(stringFour)) {
            cacheKeys.add(stringFour);
        }

        return generateCacheKey(cacheKeys);
    }

    /**
     * Generate a simple cache key based on string values
     *
     * @param cacheKeys
     * @return cache key
     */
    public static String generateCacheKey(List<String> cacheKeys) {
        if (!cacheEnabled) {
            return "";
        }

        StringBuilder cacheKey = new StringBuilder();
        for (String value : cacheKeys) {
            if (cacheKey.length() > 0) {
                cacheKey.append("-");
            }
            cacheKey.append(value);
        }

        return cacheKey.toString();
    }

    /**
     * Set the state of the cache
     *
     * @param cacheState
     */
    public static void setCacheState(boolean cacheState) {
        cacheEnabled = cacheState;
    }

    public static boolean initCacheState() {
        return PropertiesUtil.getBooleanProperty("mjb.cache", Boolean.TRUE);
    }

    /**
     * Called when running low on memory, clear the cache and turn off the
     * caching routine Also print out a message to warn the user
     */
    public static void purgeCache() {
        if (cacheEnabled) {
            LOG.debug("Cache: Disabling cache due to low memory.");
            setCacheState(false);
            clear();
        }
    }

    /**
     * Clear the cache
     */
    public static void clear() {
        LOG.debug("Cache: Clearing cache");
        CACHE.clear();
    }
}
