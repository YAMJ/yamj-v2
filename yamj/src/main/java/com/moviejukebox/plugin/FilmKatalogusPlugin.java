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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Film Katalogus Plugin for Hungarian language
 *
 * Contains code for an alternate plugin for fetching information on movies in
 * Hungarian
 *
 * @author pbando12@gmail.com
 *
 */
public class FilmKatalogusPlugin extends ImdbPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(FilmKatalogusPlugin.class);
    public static final String FILMKAT_PLUGIN_ID = "filmkatalogus";
    private final TheTvDBPlugin tvdb;
    private static final String mjbProxyHost = PropertiesUtil.getProperty("mjb.ProxyHost");
    private static final String mjbProxyPort = PropertiesUtil.getProperty("mjb.ProxyPort");

    public FilmKatalogusPlugin() {
        super(); // use IMDB as basis
        tvdb = new TheTvDBPlugin();
    }

    @Override
    public String getPluginID() {
        return FILMKAT_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean result;

        result = super.scan(mediaFile); // use IMDB as basis
        if (!result && mediaFile.isTVShow()) {
            result = tvdb.scan(mediaFile);
        }

        // check if title or plot should be retrieved
        if (OverrideTools.checkOneOverwrite(mediaFile, FILMKAT_PLUGIN_ID, OverrideFlag.TITLE, OverrideFlag.PLOT)) {
            LOG.info("Id found in nfo = {}", mediaFile.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
            getHunPlot(mediaFile);
        }

        return result;
    }

    private void getHunPlot(Movie movie) {
        String filmKatURL = "http://filmkatalogus.hu";

        try {

            if (StringTools.isNotValidString(movie.getId(FILMKAT_PLUGIN_ID))) {

                LOG.debug("Movie title for filmkatalogus search = {}", movie.getTitle());
                HttpClient httpClient = new DefaultHttpClient();
                //httpClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
                httpClient.getParams().setParameter("http.useragent", "Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)"); //User-Agent header should be overwrittem with somehting Apache is not accepted by filmkatalogus.hu

                if (mjbProxyHost != null) {
                    HttpHost proxy = new HttpHost(mjbProxyHost, Integer.parseInt(mjbProxyPort));
                    httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
                }

                HttpPost httppost = new HttpPost("http://www.filmkatalogus.hu/kereses");

                List<NameValuePair> nameValuePairs = new ArrayList<>();
                nameValuePairs.add(new BasicNameValuePair("gyorskeres", "0"));
                nameValuePairs.add(new BasicNameValuePair("keres0", "1"));
                nameValuePairs.add(new BasicNameValuePair("szo0", movie.getTitle()));

                httppost.addHeader("Content-type", "application/x-www-form-urlencoded");
                httppost.addHeader("Accept", "text/plain");

                try {
                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "ISO-8859-2"));
                } catch (UnsupportedEncodingException ex) {
                    // writing error to Log
                    LOG.error(SystemTools.getStackTrace(ex));
                    return;

                }

                try {
                    HttpResponse response = httpClient.execute(httppost);

                    switch (response.getStatusLine().getStatusCode()) {
                        case 302:
                            filmKatURL = filmKatURL.concat(response.getHeaders("location")[0].getValue());
                            LOG.debug("FilmkatalogusURL = {}", filmKatURL);
                            break;

                        case 200:
                            HttpEntity body = response.getEntity();
                            if (body == null) {
                                return;
                            }

                            String xml = EntityUtils.toString(body);

                            int beginIndex = xml.indexOf("Találat(ok) filmek között");
                            if (beginIndex != -1) { // more then one entry found use the first one
                                beginIndex = xml.indexOf("HREF='/", beginIndex);
                                int endIndex = xml.indexOf("TITLE", beginIndex);
                                filmKatURL = "http://filmkatalogus.hu";
                                filmKatURL = filmKatURL.concat(xml.substring((beginIndex + 6), endIndex - 2));
                                LOG.debug("FilmkatalogusURL = {}", filmKatURL);
                            } else {
                                return;
                            }

                            break;

                        default:
                            return;
                    }

                } catch (ClientProtocolException ex) {
                    // writing exception to log
                    LOG.error(SystemTools.getStackTrace(ex));
                    return;
                } catch (IOException ex) {
                    // writing exception to log
                    LOG.error(SystemTools.getStackTrace(ex));
                    return;
                }
            } else {
                filmKatURL = "http://filmkatalogus.hu/f";
                filmKatURL = filmKatURL.concat(movie.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
                LOG.debug("FilmkatalogusURL = {}", filmKatURL);
            }

            String xml = webBrowser.request(filmKatURL);

            // name
            int beginIndex = xml.indexOf("<H1>");
            if (beginIndex > 0) { // exact match is found
                if (OverrideTools.checkOverwriteTitle(movie, FILMKAT_PLUGIN_ID)) {
                    int endIndex = xml.indexOf("</H1>", beginIndex);
                    movie.setTitle(xml.substring((beginIndex + 4), endIndex), FILMKAT_PLUGIN_ID);
                    LOG.trace("Title: {}", movie.getTitle());
                }

                // PLOT
                if (OverrideTools.checkOverwritePlot(movie, FILMKAT_PLUGIN_ID)) {
                    beginIndex = xml.indexOf("<DIV ALIGN=JUSTIFY>", beginIndex);
                    if (beginIndex > 0) {
                        int endIndex = xml.indexOf("</DIV>", beginIndex);
                        String plot = xml.substring((beginIndex + 19), endIndex);
                        movie.setPlot(plot, FILMKAT_PLUGIN_ID);
                        LOG.trace("Plot: {}", movie.getPlot());
                    }
                }
            }
        } catch (NumberFormatException | ParseException | IOException error) {
            LOG.error("Failed retreiving information for {}", movie.getTitle());
            LOG.error(SystemTools.getStackTrace(error));
        }
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        super.scanNFO(nfo, movie); // use IMDB as basis

        boolean result = false;
        int beginIndex = nfo.indexOf("filmkatalogus.hu/");
        if (beginIndex != -1) {
            beginIndex = nfo.indexOf("--f", beginIndex);
            if (beginIndex != -1) {
                StringTokenizer filmKatID = new StringTokenizer(nfo.substring(beginIndex + 3), "/ \n,:!&é\"'(--è_çà)=$<>");
                movie.setId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID, filmKatID.nextToken());
                LOG.debug("ID found in NFO = {}", movie.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
                result = true;
            }
        }
        return result;
    }
}
