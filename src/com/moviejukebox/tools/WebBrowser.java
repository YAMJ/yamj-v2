/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * Web browser with simple cookies support
 */
public class WebBrowser {

    private static final Logger logger = Logger.getLogger("moviejukebox");

    private Map<String, String> browserProperties;
    private Map<String, Map<String, String>> cookies;
    private String mjbProxyHost;
    private String mjbProxyPort;
    private String mjbProxyUsername;
    private String mjbProxyPassword;
    private String mjbEncodedPassword;
    private int imageRetryCount;

    private static Map<String, String> hostgrp = new HashMap<String, String>();
    private static Map<String, Semaphore> grouplimits = null;
    
    /**
     * Handle download slots allocation to avoid throttling / ban on source sites
     * Find the proper semaphore for each host:
     * - Map each unique host to a group (hostgrp)
     * - Max each group (rule) to a semaphore
     * 
     * The usage pattern is:
     * s = getSemaphore(host)
     * s.acquire (or acquireUninterruptibly)
     * <download...>
     * s.release
     * 
     * @author Gabriel Corneanu
     */
    public static synchronized Semaphore getSemaphore(String host){
    
        String semaphoreGroup;
        // First we have to read/create the rules  
        if (grouplimits == null) {
            grouplimits = new HashMap<String, Semaphore>();
            // Default, can be overridden
            grouplimits.put(".*", new Semaphore(1));
            String limitsProperty = PropertiesUtil.getProperty("mjb.MaxDownloadSlots", ".*=1");
            logger.finer("WebBrowser: Using download limits: " + limitsProperty);

            Pattern semaphorePattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
            Matcher semaphoreMatcher = semaphorePattern.matcher(limitsProperty);
            while (semaphoreMatcher.find()) {
                semaphoreGroup = semaphoreMatcher.group(1);
                try{
                    Pattern.compile(semaphoreGroup);
                    logger.finer("WebBrowser: " + semaphoreGroup + "=" + semaphoreMatcher.group(2));
                    grouplimits.put(semaphoreGroup, new Semaphore( Integer.parseInt(semaphoreMatcher.group(2))));
                } catch (Exception error) {
                    logger.finer("WebBrowser: Limit rule \"" + semaphoreGroup + "\" is not valid regexp, ignored");
                }
            }
        }

        semaphoreGroup = hostgrp.get(host);
        //first time not found, search for matching group
        if (semaphoreGroup == null ) {
            semaphoreGroup = ".*";
            for(String searchGroup : grouplimits.keySet()){
                if (host.matches(searchGroup))
                    if(searchGroup.length() > semaphoreGroup.length() )
                        semaphoreGroup = searchGroup;
            }
            logger.finer(String.format("WebBrowser: Download host: %s; rule: %s", host, semaphoreGroup));
            hostgrp.put(host, semaphoreGroup);
        }

        //there should be NO way to fail
        return grouplimits.get(semaphoreGroup);
    }
    
    
    public WebBrowser() {
        browserProperties = new HashMap<String, String>();
        browserProperties.put("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
        cookies = new HashMap<String, Map<String, String>>();

        mjbProxyHost = PropertiesUtil.getProperty("mjb.ProxyHost", null);
        mjbProxyPort = PropertiesUtil.getProperty("mjb.ProxyPort", null);
        mjbProxyUsername = PropertiesUtil.getProperty("mjb.ProxyUsername", null);
        mjbProxyPassword = PropertiesUtil.getProperty("mjb.ProxyPassword", null);
        
        imageRetryCount = Integer.parseInt(PropertiesUtil.getProperty("mjb.imageRetryCount", "3"));
        if (imageRetryCount < 1) {
            imageRetryCount = 1;
        }

        if (mjbProxyUsername != null) {
            mjbEncodedPassword = mjbProxyUsername + ":" + mjbProxyPassword;
            mjbEncodedPassword = Base64.base64Encode(mjbEncodedPassword);
        }

    }

    public String request(String url) throws IOException {
        return request(new URL(url));
    }
    
    public URLConnection openProxiedConnection(URL url) throws IOException {
        if (mjbProxyHost != null) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", mjbProxyHost);
            System.getProperties().put("proxyPort", mjbProxyPort);
        }
        
        URLConnection cnx = url.openConnection();
        
        if (mjbProxyUsername != null) {
            cnx.setRequestProperty("Proxy-Authorization", mjbEncodedPassword);
        }
        
        return cnx;
    }

    public String request(URL url) throws IOException {
        StringWriter content = null;

        // get the download limit for the host 
        Semaphore s = getSemaphore(url.getHost().toLowerCase());
        s.acquireUninterruptibly();
        try {
            content = new StringWriter();

            BufferedReader in = null;
            try {
                URLConnection cnx = openProxiedConnection(url);

                sendHeader(cnx);
                readHeader(cnx);

                in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), getCharset(cnx)));
                String line;
                while ((line = in.readLine()) != null) {
                    content.write(line);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
            return content.toString();
        } finally {
            if (content != null) {
                content.close();
            }
            s.release();
        }
    }

    /**
     * Download the image for the specified URL into the specified file.
     *
     * @throws IOException
     */
    public void downloadImage(File imageFile, String imageURL) throws IOException {
        if (mjbProxyHost != null) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", mjbProxyHost);
            System.getProperties().put("proxyPort", mjbProxyPort);
        }
        
        URL url = new URL(imageURL);
        Semaphore s = getSemaphore(url.getHost().toLowerCase());
        s.acquireUninterruptibly();
        boolean success = false;
        int retryCount = imageRetryCount;
        while (!success && retryCount > 0) {
            try{
                URLConnection cnx = url.openConnection();

                // A workaround for the need to use a referrer for thetvdb.com
                if (imageURL.toLowerCase().indexOf("thetvdb") > 0)
                    cnx.setRequestProperty("Referer", "http://forums.thetvdb.com/");
           
                if (mjbProxyUsername != null) {
                    cnx.setRequestProperty("Proxy-Authorization", mjbEncodedPassword);
                }
           
                sendHeader(cnx);
                readHeader(cnx);
           
                int reportedLength = cnx.getContentLength();
                java.io.InputStream inputStream = cnx.getInputStream(); 
                int inputStreamLength = FileTools.copy(inputStream, new FileOutputStream(imageFile));
            
                if (reportedLength < 0 || reportedLength == inputStreamLength) {
                    success = true;
                } else {
                    retryCount--;
                    logger.finest("WebBrowser: Image download attempt failed, bytes expected: " +  reportedLength + ", bytes received: " + inputStreamLength);
                }
            } finally {
                s.release();
            }
        }
        if (!success) {
            logger.finest("WebBrowser: Failed " + imageRetryCount + " times to download image, aborting. URL: " + imageURL);
        }
    }

    private void sendHeader(URLConnection cnx) {
        // send browser properties
        for (Map.Entry<String, String> browserProperty : browserProperties.entrySet()) {
            cnx.setRequestProperty(browserProperty.getKey(), browserProperty.getValue());
        }
        // send cookies
        String cookieHeader = createCookieHeader(cnx);
        if (!cookieHeader.isEmpty()) {
            cnx.setRequestProperty("Cookie", cookieHeader);
        }
    }

    private String createCookieHeader(URLConnection cnx) {
        String host = cnx.getURL().getHost();
        StringBuilder cookiesHeader = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> domainCookies : cookies.entrySet()) {
            if (host.endsWith(domainCookies.getKey())) {
                for (Map.Entry<String, String> cookie : domainCookies.getValue().entrySet()) {
                    cookiesHeader.append(cookie.getKey());
                    cookiesHeader.append("=");
                    cookiesHeader.append(cookie.getValue());
                    cookiesHeader.append(";");
                }
            }
        }
        if (cookiesHeader.length() > 0) {
            // remove last ; char
            cookiesHeader.deleteCharAt(cookiesHeader.length() - 1);
        }
        return cookiesHeader.toString();
    }

    private void readHeader(URLConnection cnx) {
        // read new cookies and update our cookies
        for (Map.Entry<String, List<String>> header : cnx.getHeaderFields().entrySet()) {
            if ("Set-Cookie".equals(header.getKey())) {
                for (String cookieHeader : header.getValue()) {
                    String[] cookieElements = cookieHeader.split(" *; *");
                    if (cookieElements.length >= 1) {
                        String[] firstElem = cookieElements[0].split(" *= *");
                        String cookieName = firstElem[0];
                        String cookieValue = firstElem.length > 1 ? firstElem[1] : null;
                        String cookieDomain = null;
                        // find cookie domain
                        for (int i = 1; i < cookieElements.length; i++) {
                            String[] cookieElement = cookieElements[i].split(" *= *");
                            if ("domain".equals(cookieElement[0])) {
                                cookieDomain = cookieElement.length > 1 ? cookieElement[1] : null;
                                break;
                            }
                        }
                        if (cookieDomain == null) {
                            // if domain isn't set take current host
                            cookieDomain = cnx.getURL().getHost();
                        }
                        Map<String, String> domainCookies = cookies.get(cookieDomain);
                        if (domainCookies == null) {
                            domainCookies = new HashMap<String, String>();
                            cookies.put(cookieDomain, domainCookies);
                        }
                        // add or replace cookie
                        domainCookies.put(cookieName, cookieValue);
                    }
                }
            }
        }
    }

    private Charset getCharset(URLConnection cnx) {
        Charset charset = null;
        // content type will be string like "text/html; charset=UTF-8" or "text/html"
        String contentType = cnx.getContentType();
        if (contentType != null) {
            // changed 'charset' to 'harset' in regexp because some sites send 'Charset'
            Matcher m = Pattern.compile("harset *=[ '\"]*([^ ;'\"]+)[ ;'\"]*").matcher(contentType);
            if (m.find()) {
                String encoding = m.group(1);
                try {
                    charset = Charset.forName(encoding);
                } catch (UnsupportedCharsetException error) {
                    // there will be used default charset
                }
            }
        }
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        
        // logger.finest("Detected charset " + charset);
        return charset;
    }
}
