package com.moviejukebox.tools;

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

    public Cache() {
    }
  
    /**
     * Add an item to the cache. 
     * If the item currently exists in the cache it will be removed before being added.
     * @param key
     * @param value
     */
    public static void addToCache(String key, Object value) {
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
        if (mjbCache.contains(key)) {
            // logger.debug("*** Cache (Remove): Removing key " + key); // XXX DEBUG
            mjbCache.remove(key);
        } else {
            // logger.debug("*** Cache (Remove): Nothing to remove for " + key); // XXX DEBUG
        }
    }

    /**
     * Generate a simple cache key based on three string attributes
     * @param stringOne
     * @param stringTwo
     * @param stringThree
     * @return
     */
    public static String generateCacheKey(String stringOne, String stringTwo, String stringThree) {
        StringBuffer sb = new StringBuffer();
        sb.append(stringOne).append("-");
        sb.append(stringTwo).append("-");
        sb.append(stringThree);
        // logger.debug("*** Cache: Generating cache key of: " + sb.toString()); // XXX Debug
        return sb.toString();
    }
    
    /**
     * Clear the cache
     */
    public static void clear() {
        logger.debug("Cache: Clearing cache");
        mjbCache.clear();
    }
}
