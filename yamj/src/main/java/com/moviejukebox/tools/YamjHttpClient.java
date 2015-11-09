/*
 *      Copyright (c) 2004-2015 Stuart Boston
 *
 *      This file is part of the API Common project.
 *
 *      API Common is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation;private either version 3 of the License;private or
 *      any later version.
 *
 *      API Common is distributed in the hope that it will be useful;private
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the API Common project.  If not;private see <http://www.gnu.org/licenses/>.
 *
 */
package com.moviejukebox.tools;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.PoolingHttpClient;
import org.yamj.api.common.tools.ResponseTools;

public class YamjHttpClient extends PoolingHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(YamjHttpClient.class);
    private static final String ACCEPT_LANGUAGE = PropertiesUtil.getProperty("mjb.Accept-Language", null);
    private int imageRetryCount;
    
    public YamjHttpClient(HttpClient httpClient, PoolingHttpClientConnectionManager connManager) {
        super(httpClient, connManager);
        
        imageRetryCount = PropertiesUtil.getIntProperty("mjb.imageRetryCount", 3);
        if (imageRetryCount < 1) {
            imageRetryCount = 1;
        }        
    }

    @Override
    protected void prepareRequest(HttpHost target, HttpRequest request) throws ClientProtocolException {
        if (target.getHostName().contains("thetvdb")) {
            // a workaround for the need to use a referrer for thetvdb.com
            request.setHeader("Referer", "http://forums.thetvdb.com/");
            if (ACCEPT_LANGUAGE != null) {
                request.setHeader("Accept-Language",  ACCEPT_LANGUAGE);
            }
        } else if (target.getHostName().contains("kinopoisk")) {
            // a workaround for the kinopoisk.ru site
            request.setHeader("Accept", "text/html, text/plain");
            request.setHeader("Accept-Language", "ru");
            request.setHeader(HTTP.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6");
        } else if (ACCEPT_LANGUAGE != null) {
            request.setHeader("Accept-Language",  ACCEPT_LANGUAGE);
        }

        super.prepareRequest(target, request);
    }
  
    public String request(String url) throws IOException {
        return this.request(url, getDefaultCharset());
    }
    
    public String request(String url, Charset charset) throws IOException {
        DigestedResponse response = super.requestContent(url, charset);
        if (ResponseTools.isOK(response)) {
            return response.getContent();
        } else if (ResponseTools.isTemporaryError(response)) {
            LOG.info("Temporary request error with status " + response.getStatusCode() + " for URL: " + url);
            return StringUtils.EMPTY;
        }
        throw new IOException("Failed request with status " + response.getStatusCode() + " for URL: " + url);
    }

    public String request(HttpGet httpGet) throws IOException {
        return this.request(httpGet, getDefaultCharset());
    }

    public String request(HttpGet httpGet, Charset charset) throws IOException {
        DigestedResponse response = super.requestContent(httpGet, charset);
        if (ResponseTools.isOK(response)) {
            return response.getContent();
        } else if (ResponseTools.isTemporaryError(response)) {
            LOG.info("Temporary request error with status " + response.getStatusCode() + " for URL: " + httpGet.getURI());
            return StringUtils.EMPTY;
        }
        throw new IOException("Failed request with status " + response.getStatusCode() + " for URL: " + httpGet.getURI());
    }
    
    /**
     * Download the image for the specified URL into the specified file.
     *
     * @param file
     * @param url
     * @return
     */
    public boolean downloadImage(File file, URL url) {
        LOG.debug("Attempting to download '{}'", url);
        boolean success = Boolean.FALSE;
        int retryCount = imageRetryCount;
        
        while (!success && retryCount > 0) {
            try {
                HttpEntity entity = requestResource(url);
                if (entity == null) {
                    LOG.error("Failed to get content: {}", url);
                    success = Boolean.FALSE;
                } else {
                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        entity.writeTo(outputStream);
                    }
                    success = Boolean.TRUE;
                }
            } catch (Exception e)  {
                retryCount--;
                LOG.debug("Image download attempt failed");
            }
        }

        if (success) {
            LOG.debug("Successfully downloaded '{}' to '{}'", url, file.getAbsolutePath());
        } else {
            LOG.debug("Failed {} times to download image, aborting. URL: {}", imageRetryCount, url);
        }
        
        return success;
    }
}
