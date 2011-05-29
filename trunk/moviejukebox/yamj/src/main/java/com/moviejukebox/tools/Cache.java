package com.moviejukebox.tools;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * Utility class to provide a caching mechanism for data across threads
 * Initially this will be stored in memory, but ideally should be cached to a database
 * Many sites provide a "last modified" date/time attribute, so we should consider also caching that in the database
 * 
 * @author Stuart.Boston
 *
 */
public class Cache {
    private final static ConcurrentHashMap<String, Object> mjbCache = new ConcurrentHashMap<String, Object>();
    private static Logger logger = Logger.getLogger(Cache.class);
    private static boolean cacheEnabled = true;
    
    public Cache() {
        setCacheState(PropertiesUtil.getBooleanProperty("mjb.cache", "true"));
    }
  
    /**
     * Add an item to the cache. 
     * If the item currently exists in the cache it will be removed before being added.
     * @param key
     * @param value
     */
    public static void addToCache(String key, Object value) {
        if (!cacheEnabled) {
            return;
        }
        
        if (mjbCache.containsKey(key)) {
            logger.debug("Cache (Add): Already contains object with key " + key + " overwriting...");
            mjbCache.remove(key);
        } else {
            logger.debug("Cache (Add): Adding object for key " + key);
        }
        mjbCache.put(key, value);
        
        return;
    }
    
    /**
     * Get an item from the cache
     * @param key
     * @return
     */
    public static Object getFromCache(String key) {
        if (!cacheEnabled) {
            return null;
        }
        
        if (mjbCache.containsKey(key)) {
            logger.debug("Cache (Get): Got object for " + key);
            return mjbCache.get(key);
        } else {
            logger.debug("Cache (Get): No object found for " + key);
        }
        
        return null;
    }
    
    /**
     * Delete an item from the cache
     * @param key
     */
    public static void removeFromCache(String key) {
        if (!cacheEnabled) {
            return;
        }
        
        if (mjbCache.contains(key)) {
            // logger.debug("*** Cache (Remove): Removing key " + key); // XXX DEBUG
            mjbCache.remove(key);
        } else {
            // logger.debug("*** Cache (Remove): Nothing to remove for " + key); // XXX DEBUG
        }
    }

    public static String generateCacheKey(String stringOne, String stringTwo) {
        return generateCacheKey(stringOne, stringOne, null, null);
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
     * @return cache key
     */
    public static String generateCacheKey(ArrayList<String> cacheKeys) {
        if (!cacheEnabled) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String value : cacheKeys) {
            if (!first) {
                sb.append("-");
            }
            sb.append(value);
        }
        
        // logger.debug("*** Cache: Generating cache key of: " + sb.toString()); // XXX Debug
        return sb.toString();
    }

    /**
     * Set the state of the cache
     */
    public static void setCacheState(boolean cacheState) {
        cacheEnabled = cacheState;
    }
    
    /**
     * Called when running low on memory, clear the cache and turn off the caching routine
     * Also print out a message to warn the user
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
