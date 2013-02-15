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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.OverrideFlag;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.net.URLEncoder;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * Film Katalogus Plugin for Hungarian language
 *
 * Contains code for an alternate plugin for fetching information on movies in Hungarian
 *
 * @author pbando12@gmail.com
 *
 */
public class FilmKatalogusPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(FilmKatalogusPlugin.class);
    private static final String LOG_MESSAGE = "FilmKatalogusPlugin: ";
    public static final String FILMKAT_PLUGIN_ID = "filmkatalogus";
    private TheTvDBPlugin tvdb;

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
        if (result == false && mediaFile.isTVShow()) {
            result = tvdb.scan(mediaFile);
        }

        // check if title or plot should be retrieved
        if (OverrideTools.checkOneOverwrite(mediaFile, FILMKAT_PLUGIN_ID, OverrideFlag.TITLE, OverrideFlag.PLOT)) {
            logger.info(LOG_MESSAGE + "Id found in nfo = " + mediaFile.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
            getHunPlot(mediaFile);
        }

        return result;
    }

    private void getHunPlot(Movie movie) {
        try {
            String filmKatURL;

            if (StringTools.isNotValidString(movie.getId(FILMKAT_PLUGIN_ID))) {
                filmKatURL = "http://filmkatalogus.hu/kereses?keres0=1&szo0=";
                filmKatURL = filmKatURL.concat(URLEncoder.encode(movie.getTitle(), "ISO-8859-2"));
            } else {
                filmKatURL = "http://filmkatalogus.hu/f";
                filmKatURL = filmKatURL.concat(movie.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
            }

            String xml = webBrowser.request(filmKatURL);

            // name
            int beginIndex = xml.indexOf("<H1>");
            if (beginIndex > 0) { // exact match is found
                if (OverrideTools.checkOverwriteTitle(movie, FILMKAT_PLUGIN_ID)) {
                    int endIndex = xml.indexOf("</H1>", beginIndex);
                    movie.setTitle(new String(xml.substring((beginIndex + 4), endIndex)), FILMKAT_PLUGIN_ID);
                }

                // PLOT
                if (OverrideTools.checkOverwritePlot(movie, FILMKAT_PLUGIN_ID)) {
                    beginIndex = xml.indexOf("<DIV ALIGN=JUSTIFY>", beginIndex);
                    int endIndex = xml.indexOf("</DIV>", beginIndex);
                    String plot = new String(xml.substring((beginIndex + 19), endIndex));
                    movie.setPlot(plot, FILMKAT_PLUGIN_ID);
                }
            }

            beginIndex = xml.indexOf("Találat(ok) filmek között");
            if (beginIndex != -1) { // more then one entry found use the first one
                beginIndex = xml.indexOf("HREF='/", beginIndex);
                int endIndex = xml.indexOf("TITLE", beginIndex);
                filmKatURL = "http://filmkatalogus.hu";
                filmKatURL = filmKatURL.concat(new String(xml.substring((beginIndex + 6), endIndex - 2)));
                xml = webBrowser.request(filmKatURL);

                // name
                beginIndex = xml.indexOf("<H1>");
                if (beginIndex != -1) {
                    if (OverrideTools.checkOverwriteTitle(movie, FILMKAT_PLUGIN_ID)) {
                        endIndex = xml.indexOf("</H1>", beginIndex);
                        movie.setTitle(new String(xml.substring((beginIndex + 4), endIndex)), FILMKAT_PLUGIN_ID);
                    }

                    // PLOT
                    if (OverrideTools.checkOverwritePlot(movie, FILMKAT_PLUGIN_ID)) {
                        beginIndex = xml.indexOf("<DIV ALIGN=JUSTIFY>", beginIndex);
                        endIndex = xml.indexOf("</DIV>", beginIndex);
                        String plot = new String(xml.substring((beginIndex + 19), endIndex));
                        movie.setPlot(plot, FILMKAT_PLUGIN_ID);
                    }
                }
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving information for " + movie.getTitle());
            logger.error(SystemTools.getStackTrace(error));
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
                logger.debug(LOG_MESSAGE + "Id found in nfo = " + movie.getId(FilmKatalogusPlugin.FILMKAT_PLUGIN_ID));
                result = true;
            }
        }
        return result;
    }
}
