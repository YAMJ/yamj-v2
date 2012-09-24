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
package com.moviejukebox.tools;

import com.moviejukebox.model.Movie;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Web browser with simple cookies support
 */
public class WebBrowser {

    private static final Logger logger = Logger.getLogger(WebBrowser.class);
    private static final String logMessage = "WebBrowser: ";
    private Map<String, String> browserProperties;
    private Map<String, Map<String, String>> cookies;
    private static String mjbProxyHost = PropertiesUtil.getProperty("mjb.ProxyHost", null);
    private static String mjbProxyPort = PropertiesUtil.getProperty("mjb.ProxyPort", null);
    private static String mjbProxyUsername = PropertiesUtil.getProperty("mjb.ProxyUsername", null);
    private static String mjbProxyPassword = PropertiesUtil.getProperty("mjb.ProxyPassword", null);
    private static String mjbEncodedPassword = encodePassword();
    private static int mjbTimeoutConnect = PropertiesUtil.getIntProperty("mjb.Timeout.Connect", "25000");
    private static int mjbTimeoutRead = PropertiesUtil.getIntProperty("mjb.Timeout.Read", "90000");
    private int imageRetryCount;

    public WebBrowser() {
        browserProperties = new HashMap<String, String>();
        browserProperties.put("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
        String browserLanguage = PropertiesUtil.getProperty("mjb.Accept-Language", null);
        if (StringUtils.isNotBlank(browserLanguage)) {
            browserProperties.put("Accept-Language", browserLanguage.trim());
        }

        cookies = new HashMap<String, Map<String, String>>();

        imageRetryCount = PropertiesUtil.getIntProperty("mjb.imageRetryCount", "3");
        if (imageRetryCount < 1) {
            imageRetryCount = 1;
        }

        if (logger.isTraceEnabled()) {
            showStatus();
        }
    }

    private static String encodePassword() {
        if (mjbProxyUsername != null) {
            return ("Basic " + new String(Base64.encodeBase64((mjbProxyUsername + ":" + mjbProxyPassword).getBytes())));
        } else {
            return "";
        }
    }

    public String request(String url) throws IOException {
        return request(new URL(url));
    }

    public String request(String url, Charset charset) throws IOException {
        return request(new URL(url), charset);
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

        cnx.setConnectTimeout(mjbTimeoutConnect);
        cnx.setReadTimeout(mjbTimeoutRead);

        return cnx;
    }

    public String request(URL url) throws IOException {
        return request(url, null);
    }

    public String request(URL url, Charset charset) throws IOException {
        logger.debug(logMessage + "Requesting " + url.toString());

        // get the download limit for the host
        ThreadExecutor.enterIO(url);
        StringWriter content = new StringWriter(10 * 1024);
        try {

            URLConnection cnx = null;

            try {
                cnx = openProxiedConnection(url);

                sendHeader(cnx);
                readHeader(cnx);

                BufferedReader in = null;
                try {

                    // If we fail to get the URL information we need to exit gracefully
                    if (charset == null) {
                        in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), getCharset(cnx)));
                    } else {
                        in = new BufferedReader(new InputStreamReader(cnx.getInputStream(), charset));
                    }

                    String line;
                    while ((line = in.readLine()) != null) {
                        content.write(line);
                    }
                    // Attempt to force close connection
                    // We have HTTP connections, so these are always valid
                    content.flush();
                } catch (FileNotFoundException ex) {
                    logger.error(logMessage + "URL not found: " + url.toString());
                } catch (IOException ex) {
                    logger.error(logMessage + "Error getting URL " + url.toString() + ", " + ex.getMessage());
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (SocketTimeoutException ex) {
                logger.error(logMessage + "Timeout Error with " + url.toString());
            } finally {
                if (cnx != null) {
                    if (cnx instanceof HttpURLConnection) {
                        ((HttpURLConnection) cnx).disconnect();
                    }
                }
            }
            return content.toString();
        } finally {
            content.close();
            ThreadExecutor.leaveIO();
        }
    }

    /**
     * Download the image for the specified URL into the specified file.
     *
     * @throws IOException
     */
    public void downloadImage(File imageFile, String imageURL) throws IOException {

        String fixedImageURL;
        if (imageURL.contains(" ")) {
            fixedImageURL = imageURL.replaceAll(" ", "%20");
        } else {
            fixedImageURL = imageURL;
        }

        URL url = new URL(fixedImageURL);

        ThreadExecutor.enterIO(url);
        boolean success = false;
        int retryCount = imageRetryCount;
        try {
            while (!success && retryCount > 0) {
                URLConnection cnx = openProxiedConnection(url);

                sendHeader(cnx);
                readHeader(cnx);

                int reportedLength = cnx.getContentLength();
                java.io.InputStream inputStream = cnx.getInputStream();
                int inputStreamLength = FileTools.copy(inputStream, new FileOutputStream(imageFile));

                if (reportedLength < 0 || reportedLength == inputStreamLength) {
                    success = true;
                } else {
                    retryCount--;
                    logger.debug(logMessage + "Image download attempt failed, bytes expected: " + reportedLength + ", bytes received: " + inputStreamLength);
                }
            }
        } finally {
            ThreadExecutor.leaveIO();
        }

        if (!success) {
            logger.debug(logMessage + "Failed " + imageRetryCount + " times to download image, aborting. URL: " + imageURL);
        }
    }

    /**
     * Check the URL to see if it's one of the special cases that needs to be
     * worked around
     *
     * @param URL The URL to check
     * @param cnx The connection that has been opened
     */
    private void checkRequest(URLConnection checkCnx) {
        String checkUrl = checkCnx.getURL().getHost().toLowerCase();

        // TODO: Move these workarounds into a property file so they can be overridden at runtime

        // A workaround for the need to use a referrer for thetvdb.com
        if (checkUrl.contains("thetvdb")) {
            checkCnx.setRequestProperty("Referer", "http://forums.thetvdb.com/");
        }

        // A workaround for the kinopoisk.ru site
        if (checkUrl.contains("kinopoisk")) {
            checkCnx.setRequestProperty("Accept", "text/html, text/plain");
            checkCnx.setRequestProperty("Accept-Language", "ru");
            checkCnx.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        }
    }

    private void sendHeader(URLConnection cnx) {
        // send browser properties
        for (Map.Entry<String, String> browserProperty : browserProperties.entrySet()) {
            cnx.setRequestProperty(browserProperty.getKey(), browserProperty.getValue());

            if (logger.isTraceEnabled()) {
                logger.trace(logMessage + "setRequestProperty:" + browserProperty.getKey() + "='" + browserProperty.getValue() + "'");
            }
        }

        // send cookies
        String cookieHeader = createCookieHeader(cnx);
        if (!cookieHeader.isEmpty()) {
            cnx.setRequestProperty(logMessage + "Cookie", cookieHeader);
            if (logger.isTraceEnabled()) {
                logger.trace(logMessage + "Cookie:" + cookieHeader);
            }
        }

        checkRequest(cnx);
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

        // logger.debug("Detected charset " + charset);
        return charset;
    }

    /**
     * Get URL - allow to know if there is some redirect
     *
     * @param urlString
     * @return
     */
    public String getUrl(final String urlString) {
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            logger.warn(logMessage + "Unable to convert URL: " + urlString + " - Error: " + ex.getMessage());
            return Movie.UNKNOWN;
        }

        ThreadExecutor.enterIO(url);

        try {
            URLConnection cnx = openProxiedConnection(url);
            sendHeader(cnx);
            readHeader(cnx);
            return cnx.getURL().toString();
        } catch (IOException ex) {
            logger.warn(logMessage + "Unable to retrieve URL: " + urlString + " - Error: " + ex.getMessage());
            return Movie.UNKNOWN;
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

    public static String getMjbProxyHost() {
        return mjbProxyHost;
    }

    public static String getMjbProxyPort() {
        return mjbProxyPort;
    }

    public static String getMjbProxyUsername() {
        return mjbProxyUsername;
    }

    public static String getMjbProxyPassword() {
        return mjbProxyPassword;
    }

    public static int getMjbTimeoutConnect() {
        return mjbTimeoutConnect;
    }

    public static int getMjbTimeoutRead() {
        return mjbTimeoutRead;
    }

    public static void showStatus() {
        if (mjbProxyHost != null) {
            logger.debug(logMessage + "Proxy Host: " + mjbProxyHost);
            logger.debug(logMessage + "Proxy Port: " + mjbProxyPort);
        } else {
            logger.debug(logMessage + "No proxy set");
        }

        if (mjbProxyUsername != null) {
            logger.debug(logMessage + "Proxy Host: " + mjbProxyUsername);
            if (mjbProxyPassword != null) {
                logger.debug(logMessage + "Proxy Password: IS SET");
            }
        } else {
            logger.debug(logMessage + "No Proxy username ");
        }

        logger.debug(logMessage + "Connect Timeout: " + mjbTimeoutConnect);
        logger.debug(logMessage + "Read Timeout   : " + mjbTimeoutRead);
    }
}
