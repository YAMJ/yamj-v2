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
package com.moviejukebox.allocine;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;

/**
 * Abstract implementation for Allocine API; common methods for XML and JSON.
 *
 * @author modmax
 */
public abstract class AbstractAllocineAPI implements AllocineAPIHelper {

    private final String apiKey;
    private final String format;
    private Proxy proxy;
    
    /**
     * @param apiKey The API key for allocine
     * @param the format to use
     */
    public AbstractAllocineAPI(String apiKey, String format) {
        this.apiKey = apiKey;
        this.format = format;
    }

    @Override
    public final void setProxy(String proxyHost, int proxyPort) {
        if (StringUtils.isNotBlank(proxyHost) && (proxyPort > -1)) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }
    }

    private URLConnection connect(URL url) throws IOException {
        if (proxy == null) {
            return url.openConnection();
        } 
        return url.openConnection(proxy);
    }
    
    protected void closeInputStream(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignore) {
                // ignore this error
            }
        }
    }
    
    protected URLConnection connectSearchMovieInfos(String query) throws IOException {
        String encode = URLEncoder.encode(query, "UTF-8");
        URL url = new URL("http://api.allocine.fr/rest/v3/search?partner=" + apiKey + "&format=" + format + "&filter=movie&q=" + encode);
        return connect(url);
    }

    protected URLConnection connectSearchTvseriesInfos(String query) throws IOException {
        String encode = URLEncoder.encode(query, "UTF-8");
        URL url = new URL("http://api.allocine.fr/rest/v3/search?partner=" + apiKey + "&format=" + format + "&filter=tvseries&q=" + encode);
        return connect(url);
    }

    protected URLConnection connectGetMovieInfos(String allocineId) throws IOException {
        // HTML tags are remove from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/movie?partner=" + apiKey + "&profile=large&mediafmt=mp4-lc&format=" + format + "&filter=movie&striptags=synopsis,synopsisshort&code=" + allocineId);
        return connect(url);
    }

    protected URLConnection connectGetTvSeriesInfos(String allocineId) throws IOException {
        // HTML tags are remove from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/tvseries?partner=" + apiKey + "&profile=large&mediafmt=mp4-lc&format=" + format + "&filter=movie&striptags=synopsis,synopsisshort&code=" + allocineId);
        return connect(url);
    }

    protected URLConnection connectGetTvSeasonInfos(Integer seasonCode) throws IOException {
        // HTML tags are remove from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/season?partner=" + apiKey + "&profile=large&mediafmt=mp4-lc&format=" + format + "&filter=movie&striptags=synopsis,synopsisshort&code=" + seasonCode);
        return connect(url);
    }
}
