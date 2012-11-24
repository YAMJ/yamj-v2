/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;
import com.omertron.subbabaapi.SubBabaApi;
import com.omertron.subbabaapi.model.SearchType;
import com.omertron.subbabaapi.model.SubBabaContent;
import com.omertron.subbabaapi.model.SubBabaMovie;
import java.util.List;
import org.apache.log4j.Logger;

public class SubBabaPosterPlugin extends AbstractMoviePosterPlugin implements ITvShowPosterPlugin {

    private static final Logger logger = Logger.getLogger(SubBabaPosterPlugin.class);
    private static final String LOG_MESSAGE = "SubBabaPosterPlugin: ";
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_SubBaba");
    private SubBabaApi subBaba;

    public SubBabaPosterPlugin() {
        super();

        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }

        subBaba = new SubBabaApi(API_KEY);

        // We need to set the proxy parameters if set.
        subBaba.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        subBaba.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    /**
     * Get the Sub-Baba ID for the movie
     *
     * @param title
     * @param year
     * @return
     */
    @Override
    public String getIdFromMovieInfo(String title, String year) {
        if (StringTools.isNotValidString(title)) {
            return Movie.UNKNOWN;
        }

        logger.debug(LOG_MESSAGE + "Searching for title '" + title + "' with year '" + year + "'");
        // Use the ALL search type because we don't care what type there is
        List<SubBabaMovie> sbMovies = subBaba.searchByEnglishName(title, SearchType.ALL);

        if (sbMovies != null && !sbMovies.isEmpty()) {
            logger.debug(LOG_MESSAGE + "Found " + sbMovies.size() + " movies");
            for (SubBabaMovie sbm : sbMovies) {
                return sbm.getImdbId();
            }
        }
        return Movie.UNKNOWN;
    }

    /**
     * Use the ID to find the poster
     *
     * @param id This should be the IMDB ID. Sub-Baba does not use it's own ID.
     * @return
     */
    @Override
    public IImage getPosterUrl(String id) {
        logger.debug(LOG_MESSAGE + "Searching for poster URL with ID: " + id);
        if (StringTools.isNotValidString(id)) {
            // Invalid ID
            return Image.UNKNOWN;
        }

        IImage posterImage = getFirstContent(id, SearchType.POSTERS);
        if (posterImage.equals(Image.UNKNOWN)) {
            // No posters found, try DVD Covers
            posterImage = getFirstContent(id, SearchType.DVD_COVERS);
            if (!posterImage.equals(Image.UNKNOWN)) {
                // Crop the image
                logger.debug(LOG_MESSAGE + "Found DVD Cover for ID " + id + ", cropping image to poster size.");
                posterImage.setSubimage("0, 0, 47, 100");
            }
        }

        if (!posterImage.equals(Image.UNKNOWN)) {
            logger.debug(LOG_MESSAGE + "Found poster URL: " + posterImage.getUrl());
        }

        return posterImage;
    }

    /**
     * Find the first piece of content that matches the search type.
     *
     * @param imdbId
     * @param searchType
     * @return
     */
    private IImage getFirstContent(String imdbId, SearchType searchType) {
        SubBabaMovie sbm = subBaba.searchByImdbId(imdbId, searchType);
        if (sbm == null) {
            // Nothing returned for the movie
            logger.debug(LOG_MESSAGE + "No information found for movie ID " + imdbId);
            return Image.UNKNOWN;
        }

        List<SubBabaContent> content = sbm.getContent();

        if (content == null || content.isEmpty()) {
            // Nothing returned for the movie
            logger.debug(LOG_MESSAGE + "No " + searchType.toString() + " found for movie ID " + imdbId);
            return Image.UNKNOWN;
        }

        // return the first poster's download URL
        return new Image(content.get(0).getDownload());
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        return getIdFromMovieInfo(title, year);
    }

    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(title, year);
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }

    @Override
    public String getName() {
        return "subbaba";
    }
}
