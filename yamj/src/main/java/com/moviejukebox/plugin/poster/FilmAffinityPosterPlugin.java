/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
package com.moviejukebox.plugin.poster;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Image;
import com.moviejukebox.plugin.FilmAffinityInfo;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class FilmAffinityPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {
    private WebBrowser webBrowser;
    private FilmAffinityInfo filmAffinityInfo;

    public FilmAffinityPosterPlugin() {
        super();
        
        // Check to see if we are needed        
        /*if (!isNeeded()) {
            return;
        }*/
        
        webBrowser = new WebBrowser();
        filmAffinityInfo = new FilmAffinityInfo();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        return filmAffinityInfo.getIdFromMovieInfo(title, year);
    }

    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return filmAffinityInfo.getIdFromMovieInfo(title, year, tvSeason);
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuffer sb = new StringBuffer("http://www.filmaffinity.com/es/");
                sb.append(id);

                String xml = webBrowser.request(sb.toString());
                posterURL = HTMLTools.extractTag(xml, "<a class=\"lightbox\" href=\"", "\"");

            } catch (Exception e) {
                logger.error("Failed retreiving FilmAffinity poster url for movie : " + id);
                logger.error("Error : " + e.getMessage());
            }
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason));
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            return getPosterUrl(id);
        }
        return Image.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String id = ident.getId(this.getName());
            if (id != null) {
                response = id;
            }
        }
        return response;
    }
    
    @Override
    public String getName() {
        return FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID;
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

}
