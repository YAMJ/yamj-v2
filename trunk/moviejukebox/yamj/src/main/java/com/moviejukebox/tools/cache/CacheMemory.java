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
package com.moviejukebox.tools.cache;

import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

/**
 * Utility class to provide a caching mechanism for data across threads Initially this will be stored in memory, but
 * ideally should be cached to a database Many sites provide a "last modified" date/time attribute, so we should
 * consider also caching that in the database
 *
 * @author Stuart.Boston
 *
 */
public class CacheMemory {

    private static final ConcurrentHashMap<String, Object> mjbCache = new ConcurrentHashMap<String, Object>();
    private static final Logger logger = Logger.getLogger(CacheMemory.class);
    private static boolean cacheEnabled = true;

    public CacheMemory() {
        setCacheState(PropertiesUtil.getBooleanProperty("mjb.cache", "true"));
    }

    /**
     * Add an item to the cache. If the item currently exists in the cache it will be removed before being added.
     *
     * @param key
     * @param value
     */
    public static void addToCache(String key, Object value) {
        if (!cacheEnabled) {
            return;
        }

        if (mjbCache.containsKey(key)) {
            logger.debug("Cache (Add): Already contains object (" + value.getClass().getSimpleName() + ") with key " + key + " overwriting...");
            mjbCache.remove(key);
        } else {
            logger.debug("Cache (Add): Adding object (" + value.getClass().getSimpleName() + ") for key " + key);
        }
        mjbCache.put(key, value);
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

        if (mjbCache.containsKey(key)) {
            Object value = mjbCache.get(key);
            if (value == null) {
                logger.debug("Cache (Get): No object found for " + key);
            } else {
                logger.debug("Cache (Get): Got object (" + value.getClass().getSimpleName() + ") for " + key);
                return value;
            }
        } else {
            logger.debug("Cache (Get): No object found for " + key);
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

        if (mjbCache.contains(key)) {
            mjbCache.remove(key);
        }
    }

    public static String generateCacheKey(String stringOne, String stringTwo) {
        return generateCacheKey(stringOne, stringTwo, null, null);
    }

    public static String generateCacheKey(String stringOne, String stringTwo, String stringThree) {
        return generateCacheKey(stringOne, stringTwo, stringThree, null);
    }

    public static String generateCacheKey(String stringOne, String stringTwo, String stringThree, String stringFour) {
        ArrayList<String> cacheKeys = new ArrayList<String>();
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
     * @return cache key
     */
    public static String generateCacheKey(ArrayList<String> cacheKeys) {
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
     */
    public static void setCacheState(boolean cacheState) {
        cacheEnabled = cacheState;
    }

    /**
     * Called when running low on memory, clear the cache and turn off the caching routine Also print out a message to
     * warn the user
     */
    public static void purgeCache() {
        if (cacheEnabled) {
            logger.debug("Cache: Disabling cache due to low memory.");
            setCacheState(false);
            clear();
        }
    }

    /**
     * Clear the cache
     */
    public static void clear() {
        logger.debug("Cache: Clearing cache");
        mjbCache.clear();
    }
}
