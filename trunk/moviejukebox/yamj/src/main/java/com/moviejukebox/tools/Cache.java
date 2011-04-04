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
    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(Cache.class);

    public Cache() {
    }
    
    public static void addToCache(String key, Object value) {
        if (mjbCache.containsKey(key)) {
//            logger.info("*** Cache (Add): Already contains object with key " + key + " overwriting..."); // XXX DEBUG
            mjbCache.remove(key);
        } else {
//            logger.info("*** Cache (Add): Adding object for key " + key);     // XXX DEBUG
        }
        mjbCache.put(key, value);
        
        return;
    }
    
    public static Object getFromCache(String key) {
        Object mjbObject = mjbCache.get(key);
        
        if (mjbObject == null) {
//            logger.info("*** Cache (Get): No object found for " + key); // XXX DEBUG
        } else {
//            logger.info("*** Cache (Get): Got object for " + key); // XXX DEBUG
        }
        return mjbObject;
    }
    
    public static void removeFromCache(String key) {
        if (mjbCache.contains(key)) {
//            logger.info("*** Cache (Remove): Removing key " + key); // XXX DEBUG
            mjbCache.remove(key);
        } else {
//            logger.info("*** Cache (Remove): Nothing to remove for " + key); // XXX DEBUG
        }
    }

    public static String generateCacheKey(String stringOne, String stringTwo, String stringThree) {
        StringBuffer sb = new StringBuffer();
        sb.append(stringOne).append("-");
        sb.append(stringTwo).append("-");
        sb.append(stringThree);
//        logger.info("*** Generating cache key of: " + sb.toString()); // XXX Debug
        return sb.toString();
    }
}
