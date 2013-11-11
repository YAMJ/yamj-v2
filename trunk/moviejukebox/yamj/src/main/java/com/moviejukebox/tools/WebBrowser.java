/*
 *      Copyright (c) 2004-2013 YAMJ Members
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

import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
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
    private static final String LOG_MESSAGE = "WebBrowser: ";
    private final Map<String, String> browserProperties;
    private final Map<String, Map<String, String>> cookies;
    private static final String PROXY_HOST = PropertiesUtil.getProperty("mjb.ProxyHost");
    private static final String PROXY_PORT = PropertiesUtil.getProperty("mjb.ProxyPort");
    private static final String PROXY_USERNAME = PropertiesUtil.getProperty("mjb.ProxyUsername");
    private static final String PROXY_PASSWORD = PropertiesUtil.getProperty("mjb.ProxyPassword");
    private static final String ENCODED_PASSWORD = encodePassword();
    private static final int TIMEOUT_CONNECT = PropertiesUtil.getIntProperty("mjb.Timeout.Connect", 25000);
    private static final int TIMEOUT_READ = PropertiesUtil.getIntProperty("mjb.Timeout.Read", 90000);
    private int imageRetryCount;

    public WebBrowser() {
        browserProperties = new HashMap<String, String>();
        browserProperties.put("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
        String browserLanguage = PropertiesUtil.getProperty("mjb.Accept-Language", null);
        if (StringUtils.isNotBlank(browserLanguage)) {
            browserProperties.put("Accept-Language", browserLanguage.trim());
        }

        cookies = new HashMap<String, Map<String, String>>();

        imageRetryCount = PropertiesUtil.getIntProperty("mjb.imageRetryCount", 3);
        if (imageRetryCount < 1) {
            imageRetryCount = 1;
        }

        if (logger.isTraceEnabled()) {
            showStatus();
        }
    }

    public void addBrowserProperty(String key, String value) {
        this.browserProperties.put(key, value);
    }

    private static String encodePassword() {
        if (PROXY_USERNAME != null) {
            return ("Basic " + new String(Base64.encodeBase64((PROXY_USERNAME + ":" + PROXY_PASSWORD).getBytes())));
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
        if (PROXY_HOST != null) {
            System.getProperties().put("proxySet", TRUE);
            System.getProperties().put("proxyHost", PROXY_HOST);
            System.getProperties().put("proxyPort", PROXY_PORT);
        }

        URLConnection cnx = url.openConnection();

        if (PROXY_USERNAME != null) {
            cnx.setRequestProperty("Proxy-Authorization", ENCODED_PASSWORD);
        }

        cnx.setConnectTimeout(TIMEOUT_CONNECT);
        cnx.setReadTimeout(TIMEOUT_READ);

        return cnx;
    }

    public String request(URL url) throws IOException {
        return request(url, null);
    }

    public String request(URL url, Charset charset) throws IOException {
        logger.debug(LOG_MESSAGE + "Requesting " + url.toString());

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
                    logger.error(LOG_MESSAGE + "URL not found: " + url.toString());
                } catch (IOException ex) {
                    logger.error(LOG_MESSAGE + "Error getting URL " + url.toString() + ", " + ex.getMessage());
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (SocketTimeoutException ex) {
                logger.error(LOG_MESSAGE + "Timeout Error with " + url.toString());
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
     * @param imageFile
     * @param imageURL
     * @return
     * @throws IOException
     */
    public boolean downloadImage(File imageFile, String imageURL) throws IOException {

        String fixedImageURL;
        if (imageURL.contains(" ")) {
            fixedImageURL = imageURL.replaceAll(" ", "%20");
        } else {
            fixedImageURL = imageURL;
        }

        URL url = new URL(fixedImageURL);

        logger.debug(LOG_MESSAGE + "Attempting to download '" + fixedImageURL + "'");

        ThreadExecutor.enterIO(url);
        boolean success = Boolean.FALSE;
        int retryCount = imageRetryCount;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        int reportedLength = 0;
        try {
            while (!success && retryCount > 0) {
                URLConnection cnx = openProxiedConnection(url);

                sendHeader(cnx);
                readHeader(cnx);

                reportedLength = cnx.getContentLength();
                inputStream = cnx.getInputStream();
                outputStream = new FileOutputStream(imageFile);
                int inputStreamLength = FileTools.copy(inputStream, outputStream);

                if (reportedLength < 0 || reportedLength == inputStreamLength) {
                    success = Boolean.TRUE;
                } else {
                    retryCount--;
                    logger.debug(LOG_MESSAGE + "Image download attempt failed, bytes expected: " + reportedLength + ", bytes received: " + inputStreamLength);
                }
            }
        } finally {
            ThreadExecutor.leaveIO();

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }

        if (success) {
            logger.debug(LOG_MESSAGE + "Successfully downloaded '" + imageURL + "' to '" + imageFile.getAbsolutePath() + "', Size: " + reportedLength);
        } else {
            logger.debug(LOG_MESSAGE + "Failed " + imageRetryCount + " times to download image, aborting. URL: " + imageURL);
        }
        return success;
    }

    /**
     * Check the URL to see if it's one of the special cases that needs to be worked around
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
                logger.trace(LOG_MESSAGE + "setRequestProperty:" + browserProperty.getKey() + "='" + browserProperty.getValue() + "'");
            }
        }

        // send cookies
        String cookieHeader = createCookieHeader(cnx);
        if (!cookieHeader.isEmpty()) {
            cnx.setRequestProperty(LOG_MESSAGE + "Cookie", cookieHeader);
            if (logger.isTraceEnabled()) {
                logger.trace(LOG_MESSAGE + "Cookie:" + cookieHeader);
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
            logger.warn(LOG_MESSAGE + "Unable to convert URL: " + urlString + " - Error: " + ex.getMessage());
            return Movie.UNKNOWN;
        }

        ThreadExecutor.enterIO(url);

        try {
            URLConnection cnx = openProxiedConnection(url);
            sendHeader(cnx);
            readHeader(cnx);
            return cnx.getURL().toString();
        } catch (IOException ex) {
            logger.warn(LOG_MESSAGE + "Unable to retrieve URL: " + urlString + " - Error: " + ex.getMessage());
            return Movie.UNKNOWN;
        } finally {
            ThreadExecutor.leaveIO();
        }
    }

    public static String getMjbProxyHost() {
        return PROXY_HOST;
    }

    public static String getMjbProxyPort() {
        return PROXY_PORT;
    }

    public static String getMjbProxyUsername() {
        return PROXY_USERNAME;
    }

    public static String getMjbProxyPassword() {
        return PROXY_PASSWORD;
    }

    public static int getMjbTimeoutConnect() {
        return TIMEOUT_CONNECT;
    }

    public static int getMjbTimeoutRead() {
        return TIMEOUT_READ;
    }

    public static void showStatus() {
        if (PROXY_HOST != null) {
            logger.debug(LOG_MESSAGE + "Proxy Host: " + PROXY_HOST);
            logger.debug(LOG_MESSAGE + "Proxy Port: " + PROXY_PORT);
        } else {
            logger.debug(LOG_MESSAGE + "No proxy set");
        }

        if (PROXY_USERNAME != null) {
            logger.debug(LOG_MESSAGE + "Proxy Host: " + PROXY_USERNAME);
            if (PROXY_PASSWORD != null) {
                logger.debug(LOG_MESSAGE + "Proxy Password: IS SET");
            }
        } else {
            logger.debug(LOG_MESSAGE + "No Proxy username ");
        }

        logger.debug(LOG_MESSAGE + "Connect Timeout: " + TIMEOUT_CONNECT);
        logger.debug(LOG_MESSAGE + "Read Timeout   : " + TIMEOUT_READ);
    }
}
