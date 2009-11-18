package com.moviejukebox.tools;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.LogRecord;

public class LogFormatter extends java.util.logging.Formatter 
{
    private static ArrayList<String> API_KEYS = new ArrayList<String>();
    
	public String format(LogRecord log) {
		String logMessage = log.getMessage();

		for (String ApiKey : API_KEYS) {
			logMessage = logMessage.replace(ApiKey, "..APIKEY..");
		}
		logMessage += "\n";
		
		Throwable thrown = log.getThrown();
		if (thrown != null) { 
			logMessage = logMessage + thrown.toString(); 
		}
		return logMessage;
	}
	
	/**
	 * Once the properties files have been loaded then add the API_KEYs to the static list
	 */
	public static void addApiKeys() {
        for (Map.Entry<Object, Object> propEntry : PropertiesUtil.getEntrySet()) {
        	if (propEntry.getKey().toString().toUpperCase().startsWith("API_KEY")) {
        		API_KEYS.add(propEntry.getValue().toString());
        	}
        }
        return;
	}
}
