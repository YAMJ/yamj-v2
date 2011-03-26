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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

public class ApiFilteringLayout extends PatternLayout {
    private static Pattern API_KEYS = Pattern.compile("DO_NOT_MATCH");
    
    @Override
    public String format(LoggingEvent event) {
        if (event.getMessage() instanceof String) {
            String message = event.getRenderedMessage();

            Matcher matcher = API_KEYS.matcher(message);
            if (matcher.find()) {
                String maskedMessage = matcher.replaceAll("[APIKEY]");
                
                Throwable throwable = event.getThrowableInformation() != null ? 
                        event.getThrowableInformation().getThrowable() : null;
                
                LoggingEvent maskedEvent = new LoggingEvent(event.fqnOfCategoryClass,
                        Logger.getLogger(event.getLoggerName()), event.timeStamp, 
                        event.getLevel(), maskedMessage, throwable);

                return super.format(maskedEvent);
            }
        }
        return super.format(event);
    }
    
    /**
     * Once the properties files have been loaded then add the API_KEYs to the static list
     */
    public static void addApiKeys() {
        StringBuffer apis = new StringBuffer("");
        
        for (Map.Entry<Object, Object> propEntry : PropertiesUtil.getEntrySet()) {
            if (propEntry.getKey().toString().toUpperCase().startsWith("API_KEY")) {
                apis.append("|").append(propEntry.getValue().toString());
            }
        }
        
        if (apis.length() > 0) {
            apis.deleteCharAt(0);   // Remove the first "|"
        }
        API_KEYS = Pattern.compile(apis.toString());
        
        return;
    }
}
