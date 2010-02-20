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

package com.moviejukebox.plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.StringTokenizer;


import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class FilmaffinityPlugin extends ImdbPlugin {

    public static String FILMAFFINITY_PLUGIN_ID = "filmaffinity";
    //Define plot length
    int preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));

    public FilmaffinityPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Spain");
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = true;
        String filmAffinityId = mediaFile.getId(FILMAFFINITY_PLUGIN_ID);
        if (filmAffinityId == null || filmAffinityId.equalsIgnoreCase(Movie.UNKNOWN)) {
            filmAffinityId = getFilmAffinityId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.getSeason());
        } else {
            filmAffinityId += ".html";
        }
        
        if (filmAffinityId.indexOf(".html") != -1) { // Update original title and plot (in Spanish)
            retval = updateFilmAffinityMediaInfo(mediaFile, filmAffinityId);
        }
        
        // Fill in the rest of the fields from IMDB, taking care not to allow the title to get overwritten
        boolean overrideTitle = mediaFile.isOverrideTitle();
        mediaFile.setOverrideTitle(true);
        super.scan(mediaFile);
        mediaFile.setOverrideTitle(overrideTitle);
        
        return retval;
    }

    /**
     * retrieve the imdb matching the specified movie name and year. This routine is base on a google
     * request.
     */
    private String getFilmAffinityId(String movieName, String year, int season) {
        try {
            StringBuffer sb = new StringBuffer("http://www.google.es/search?hl=es&q=");
            
            sb.append(URLEncoder.encode(movieName, "UTF-8"));
            if (season != -1) {
                sb.append("+TV");
            }
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+").append(year);
            }

            sb.append("+site%3Awww.filmaffinity.com&btnG=Buscar+con+Google&meta=");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("/es/film");
            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 8), "/\"");
            String filmAffinityId = st.nextToken();

            //if ( imdbId.startsWith( "film" ) )
            if (filmAffinityId != "") {
                logger.finest("FilmAffinity: Found id: " + filmAffinityId);
                return filmAffinityId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception error) {
            logger.severe("Failed retreiving imdb Id for movie : " + movieName);
            logger.severe("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Scan FilmAffinity html page for the specified movie
     */
    private boolean updateFilmAffinityMediaInfo(Movie movie, String filmAffinityId) {
        try {
            String xml = webBrowser.request("http://www.filmaffinity.com/es/film" + filmAffinityId);

            if (xml.contains("Serie de TV")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            if (!movie.isOverrideTitle()) {
                movie.setTitle(HTMLTools.extractTag(xml, "<td ><b>", 0, "()><-"));
            }
            
            if (movie.getPlot().equalsIgnoreCase(Movie.UNKNOWN)) {
                String plot = HTMLTools.extractTag(xml, "SINOPSIS", 4, "><|");
                if (plot.length() > preferredPlotLength) {
                    plot = plot.substring(0, preferredPlotLength - 3) + "...";
                }

                movie.setPlot(plot);
            }
            
            if (movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                String posterURL = HTMLTools.extractTag(xml, "<a class=\"lightbox\" href=\"", "\"");
                System.out.println("FilmAffinity Poster: " + posterURL);
                movie.setPosterURL(posterURL);
            }

        } catch (Exception error) {
            logger.severe("Failed retreiving filmaffinity data movie : " + movie.getId(IMDB_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return true;
    }
}


