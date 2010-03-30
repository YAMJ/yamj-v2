package com.moviejukebox.tools;

import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.LogRecord;

public class LogFormatter extends java.util.logging.Formatter 
{
    private static ArrayList<String> API_KEYS = new ArrayList<String>();
    private static String EOL = (String)java.security.AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
            return System.getProperty("line.separator");
        }
    });
    private static boolean logThreadName = false;
    private static boolean logTimeStamp  = false;

    public synchronized String format(LogRecord logRecord) {
        String logMessage = logRecord.getMessage();
        
        try {
            for (String ApiKey : API_KEYS) {
                logMessage = logMessage.replace(ApiKey, "[APIKEY]");
            }
        } catch (Exception error) {
            // We don't care about this error really.
        }
        
        logMessage = logPrefix() + logMessage + EOL;

        Throwable thrown = logRecord.getThrown();
        if (thrown != null) { 
            logMessage = logMessage + thrown.toString(); 
        }
        return logMessage;
    }
    
    private String logPrefix() {
        if (!logTimeStamp && !logThreadName) {
            return "";
        }
        
        String logPrefix = "{";
        String logSpace = "";

        if (logTimeStamp) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("kk:mm:ss");
            logPrefix += dateFormat.format(new Date());
            logSpace = " ";
        }
        
        if (logThreadName) {
            logPrefix += logSpace + Thread.currentThread().getName();
        }
        
        return logPrefix + "} ";
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
        
        logThreadName = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.logThreadName", "false"));
        logTimeStamp  = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.logTimeStamp", "false"));

        return;
    }
}
