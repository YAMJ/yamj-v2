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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.http.WebBrowserUserAgentSelector;

/**
 * Builder for a YAMJ http client.
 */
public class YamjHttpClientBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(YamjHttpClientBuilder.class);

    private static final String PROXY_HOST = PropertiesUtil.getProperty("mjb.ProxyHost", "");
    private static final int PROXY_PORT = PropertiesUtil.getIntProperty("mjb.ProxyPort", 0);
    private static final String PROXY_USERNAME = PropertiesUtil.getProperty("mjb.ProxyUsername", "");
    private static final String PROXY_PASSWORD = PropertiesUtil.getProperty("mjb.ProxyPassword", "");
    private static final int TIMEOUT_SOCKET = PropertiesUtil.getIntProperty("mjb.Timeout.Socket", 90000);
    private static final int TIMEOUT_CONNECT = PropertiesUtil.getIntProperty("mjb.Timeout.Connect", 25000);
    private static final int TIMEOUT_READ = PropertiesUtil.getIntProperty("mjb.Timeout.Read", 90000);

    private static YamjHttpClient YAMJ_HTTP_CLIENT;
    private static Lock LOCK = new ReentrantLock(true);
    private static Proxy PROXY;
    
    static {
        // create the java proxy object
        if (StringUtils.isNotBlank(PROXY_HOST) && PROXY_PORT > 0) {
            SocketAddress socketAddress = new InetSocketAddress(PROXY_HOST, PROXY_PORT);
            PROXY = new Proxy(Proxy.Type.HTTP, socketAddress);
        } else {
            PROXY = Proxy.NO_PROXY;
        }
    }
    
    public static Proxy getProxy() {
        return PROXY;
    }
    
    public static void showStatus() {
        if (LOG.isTraceEnabled()) {
            if (StringUtils.isNotBlank(PROXY_HOST) && PROXY_PORT > 0) {
                LOG.trace("Proxy Host: {}", PROXY_HOST);
                LOG.trace("Proxy Port: {}", PROXY_PORT);
            } else {
                LOG.trace("No proxy set");
            }
    
            if (StringUtils.isNotBlank(PROXY_USERNAME)) {
                LOG.trace("Proxy username: {}", PROXY_USERNAME);
                if (PROXY_PASSWORD != null) {
                    LOG.trace("Proxy password: IS SET");
                }
            } else {
                LOG.trace("No proxy username");
            }
    
            LOG.trace("Socket Timeout:  {}", TIMEOUT_SOCKET);
            LOG.trace("Connect Timeout: {}", TIMEOUT_CONNECT);
            LOG.trace("Read Timeout   : {}", TIMEOUT_READ);
        }
    }

    /**
     * Get the singleton pooling YamjHttpClient
     */
    public static YamjHttpClient getHttpClient() {
        if (YAMJ_HTTP_CLIENT == null) {
            LOCK.lock();
            try {
                if (YAMJ_HTTP_CLIENT == null) {
                    YAMJ_HTTP_CLIENT = buildHttpClient();
                }
            } finally {
                LOCK.unlock();
            }
        }
        return YAMJ_HTTP_CLIENT;
    }

    @SuppressWarnings("resource")
    private static YamjHttpClient buildHttpClient() {
        LOG.trace("Create new YAMJ http client");
        
        // create proxy
        HttpHost proxy = null;
        CredentialsProvider credentialsProvider = null;
        
        if (StringUtils.isNotBlank(PROXY_HOST) && PROXY_PORT > 0) {
            proxy = new HttpHost(PROXY_HOST, PROXY_PORT);
          
            if (StringUtils.isNotBlank(PROXY_USERNAME) && StringUtils.isNotBlank(PROXY_PASSWORD)) {
                credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(PROXY_HOST, PROXY_PORT),
                        new UsernamePasswordCredentials(PROXY_USERNAME, PROXY_PASSWORD));
            }
        }

        
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        connManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(TIMEOUT_SOCKET).build());
        connManager.setMaxTotal(20);
        connManager.setDefaultMaxPerRoute(2);
        
        CacheConfig cacheConfig = CacheConfig.custom()
                        .setMaxCacheEntries(1000)
                        .setMaxObjectSize(8192)
                        .build();
        
        HttpClientBuilder builder = CachingHttpClientBuilder.create()
                .setCacheConfig(cacheConfig)
                .setConnectionManager(connManager)
                .setProxy(proxy)
                .setDefaultCredentialsProvider(credentialsProvider)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(TIMEOUT_READ)
                        .setConnectTimeout(TIMEOUT_CONNECT)
                        .setSocketTimeout(TIMEOUT_SOCKET)
                        .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                        .setProxy(proxy)
                        .build());

        // show status
        showStatus();
        
        // build the client
        YamjHttpClient wrapper = new YamjHttpClient(builder.build(), connManager);
        wrapper.setUserAgentSelector(new WebBrowserUserAgentSelector());
        wrapper.addGroupLimit(".*", 1); // default limit, can be overwritten
        
        // First we have to read/create the rules
        String maxDownloadSlots = PropertiesUtil.getProperty("mjb.MaxDownloadSlots");
        if (StringUtils.isNotBlank(maxDownloadSlots)) {
            LOG.debug("Using download limits: {}", maxDownloadSlots);
    
            Pattern pattern = Pattern.compile(",?\\s*([^=]+)=(\\d+)");
            Matcher matcher = pattern.matcher(maxDownloadSlots);
            while (matcher.find()) {
                String group = matcher.group(1);
                try {
                    final Integer maxResults = Integer.valueOf(matcher.group(2));
                    wrapper.addGroupLimit(group, maxResults);
                    LOG.trace("Added download slot '{}' with max results {}", group, maxResults);
                } catch (NumberFormatException error) {
                    LOG.debug("Rule '{}' is no valid regexp, ignored", group);
                }
            }
        }
        
        return wrapper;
    }
}
