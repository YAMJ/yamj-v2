/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.tools;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Log4J Filtering routine to remove API keys from the output
 * @author Stuart.Boston
 *
 */
public class FilteringLayout extends PatternLayout {
    private static Pattern apiKeys = Pattern.compile("DO_NOT_MATCH");

    @Override
    public String format(LoggingEvent event) {
        if (event.getMessage() instanceof String) {
            String message = event.getRenderedMessage();

            Matcher matcher = apiKeys.matcher(message);
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
        StringBuilder apis = new StringBuilder("");

        for (Map.Entry<Object, Object> propEntry : PropertiesUtil.getEntrySet()) {
            if (propEntry.getKey().toString().toUpperCase().startsWith("API_KEY")) {
                apis.append("|").append(propEntry.getValue().toString());
            }
        }

        if (apis.length() > 0) {
            apis.deleteCharAt(0);   // Remove the first "|"
        }
        apiKeys = Pattern.compile(apis.toString());
    }
}
