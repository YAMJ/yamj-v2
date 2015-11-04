/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.model.Movie;

/**
 * Web browser with simple cookies support
 */
public class WebBrowser {

    private static final Logger LOG = LoggerFactory.getLogger(WebBrowser.class);
    private static final String PROXY_USERNAME = PropertiesUtil.getProperty("mjb.ProxyUsername", "");
    private static final String PROXY_PASSWORD = PropertiesUtil.getProperty("mjb.ProxyPassword", "");
    private static final String ENCODED_PASSWORD = encodePassword();
    private static final int TIMEOUT_CONNECT = PropertiesUtil.getIntProperty("mjb.Timeout.Connect", 25000);
    private static final int TIMEOUT_READ = PropertiesUtil.getIntProperty("mjb.Timeout.Read", 90000);

    private final Map<String, String> browserProperties;
    private final Map<String, Map<String, String>> cookies;

    public WebBrowser() {
        browserProperties = new HashMap<>();
        browserProperties.put("User-Agent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");
        String browserLanguage = PropertiesUtil.getProperty("mjb.Accept-Language", null);
        if (StringUtils.isNotBlank(browserLanguage)) {
            browserProperties.put("Accept-Language", browserLanguage.trim());
        }

        cookies = new HashMap<>();
    }

    private static String encodePassword() {
        if (PROXY_USERNAME != null) {
            return ("Basic " + new String(Base64.encodeBase64((PROXY_USERNAME + ":" + PROXY_PASSWORD).getBytes())));
        }
        return StringUtils.EMPTY;
    }

    public String request(String url, Charset charset) throws IOException {
        return request(new URL(url), charset);
    }

    public URLConnection openProxiedConnection(URL url) throws IOException {
        URLConnection cnx = url.openConnection(YamjHttpClientBuilder.getProxy());

        if (PROXY_USERNAME != null) {
            cnx.setRequestProperty("Proxy-Authorization", ENCODED_PASSWORD);
        }

        cnx.setConnectTimeout(TIMEOUT_CONNECT);
        cnx.setReadTimeout(TIMEOUT_READ);

        return cnx;
    }

    @SuppressWarnings("resource")
    public String request(URL url, Charset charset) throws IOException {
        LOG.debug("Requesting {}", url.toString());

        // get the download limit for the host
        ThreadExecutor.enterIO(url);
        StringWriter content = new StringWriter(10 * 1024);
        try {

            URLConnection cnx = null;

            try {
                cnx = openProxiedConnection(url);

                sendHeader(cnx);
                readHeader(cnx);

                InputStreamReader inputStreamReader = null;
                BufferedReader bufferedReader = null;

                try (InputStream inputStream = cnx.getInputStream()) {

                    // If we fail to get the URL information we need to exit gracefully
                    if (charset == null) {
                        inputStreamReader = new InputStreamReader(inputStream, getCharset(cnx));
                    } else {
                        inputStreamReader = new InputStreamReader(inputStream, charset);
                    }
                    bufferedReader = new BufferedReader(inputStreamReader);

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        content.write(line);
                    }

                    // Attempt to force close connection
                    // We have HTTP connections, so these are always valid
                    content.flush();

                } catch (FileNotFoundException ex) {
                    LOG.error("URL not found: {}", url.toString());
                } catch (IOException ex) {
                    LOG.error("Error getting URL {}, {}", url.toString(), ex.getMessage());
                } finally {
                    // Close resources
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (Exception ex) { /* ignore */ }
                    }
                    if (inputStreamReader != null) {
                        try {
                            inputStreamReader.close();
                        } catch (Exception ex) { /* ignore */ }
                    }
                }
            } catch (SocketTimeoutException ex) {
                LOG.error("Timeout Error with {}", url.toString());
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
     * Check the URL to see if it's one of the special cases that needs to be worked around
     *
     * @param URL The URL to check
     * @param cnx The connection that has been opened
     */
    private static void checkRequest(URLConnection checkCnx) {
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

            if (LOG.isTraceEnabled()) {
                LOG.trace("setRequestProperty:{}='{}'", browserProperty.getKey(), browserProperty.getValue());
            }
        }

        // send cookies
        String cookieHeader = createCookieHeader(cnx);
        if (!cookieHeader.isEmpty()) {
            cnx.setRequestProperty("Cookie", cookieHeader);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Cookie:{}", cookieHeader);
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
                        putCookie(cookieDomain, cookieName, cookieValue);
                    }
                }
            }
        }
    }

    public void putCookie(String cookieDomain, String cookieName, String cookieValue) {
        if (cookieDomain != null) {
            Map<String, String> domainCookies = cookies.get(cookieDomain);
            if (domainCookies == null) {
                domainCookies = new HashMap<>();
                cookies.put(cookieDomain, domainCookies);
            }
            // add or replace cookie
            domainCookies.put(cookieName, cookieValue);
        }
    }

    private static Charset getCharset(URLConnection cnx) {
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
            LOG.warn("Unable to convert URL: {} - Error: {}", urlString, ex.getMessage());
            return Movie.UNKNOWN;
        }

        ThreadExecutor.enterIO(url);

        try {
            URLConnection cnx = openProxiedConnection(url);
            sendHeader(cnx);
            readHeader(cnx);
            return cnx.getURL().toString();
        } catch (IOException ex) {
            LOG.warn("Unable to retrieve URL: {} - Error: {}", urlString, ex.getMessage());
            return Movie.UNKNOWN;
        } finally {
            ThreadExecutor.leaveIO();
        }
    }
}
