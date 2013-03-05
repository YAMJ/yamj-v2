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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Abstract implementation for Allocine API; common methods for XML and JSON.
 *
 * @author modmax
 */
public abstract class AbstractAllocineAPI implements AllocineAPIHelper {

    private final String apiKey;
    private final String format;
    
    /**
     * @param apiKey The API key for allocine
     * @param the format to use
     */
    public AbstractAllocineAPI(String apiKey, String format) {
        this.apiKey = apiKey;
        this.format = format;
    }

    @Override
    public void setProxy(String host, String port, String username, String password) {
        WebBrowser.setProxyHost(host);
        WebBrowser.setProxyPort(port);
        WebBrowser.setProxyPassword(username, password);
    }
    
    protected void close(URLConnection connection, InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignore) {
                // ignore this error
            }
        }

        if (connection != null && (connection instanceof HttpURLConnection)) {
            try {
                ((HttpURLConnection)connection).disconnect();
            } catch (Exception ignore) {
                // ignore this error
            }
        }
    }
    
    protected URLConnection connectSearchMovieInfos(String query) throws IOException {
        String encode = URLEncoder.encode(query, "UTF-8");
        URL url = new URL("http://api.allocine.fr/rest/v3/search?partner=" + apiKey + "&format=" + format + "&filter=movie&q=" + encode);
        return WebBrowser.openProxiedConnection(url);
    }

    protected URLConnection connectSearchTvseriesInfos(String query) throws IOException {
        String encode = URLEncoder.encode(query, "UTF-8");
        URL url = new URL("http://api.allocine.fr/rest/v3/search?partner=" + apiKey + "&format=" + format + "&filter=tvseries&q=" + encode);
        return WebBrowser.openProxiedConnection(url);
    }

    protected URLConnection connectGetMovieInfos(String allocineId) throws IOException {
        // HTML tags are removed from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/movie?partner=" + apiKey + "&profile=large&mediafmt=mp4-lc&format=" + format + "&filter=movie&striptags=synopsis,synopsisshort&code=" + allocineId);
        return WebBrowser.openProxiedConnection(url);
    }

    protected URLConnection connectGetTvSeriesInfos(String allocineId) throws IOException {
        // HTML tags are removed from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/tvseries?partner=" + apiKey + "&profile=large&mediafmt=mp4-lc&format=" + format + "&filter=movie&striptags=synopsis,synopsisshort&code=" + allocineId);
        return WebBrowser.openProxiedConnection(url);
    }

    protected URLConnection connectGetTvSeasonInfos(Integer seasonCode) throws IOException {
        // HTML tags are removed from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/season?partner=" + apiKey + "&profile=large&mediafmt=mp4-lc&format=" + format + "&filter=movie&striptags=synopsis,synopsisshort&code=" + seasonCode);
        return WebBrowser.openProxiedConnection(url);
    }
}
