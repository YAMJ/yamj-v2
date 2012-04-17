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
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import org.apache.log4j.Logger;

/**
 * Scanner for ClearArt artwork
 *
 * @author stuart.boston
 */
public class ClearArtScanner extends ArtworkScanner {

    private static final Logger logger = Logger.getLogger(ClearArtScanner.class);

    public ClearArtScanner() {
        super(ArtworkType.ClearArt);

        artworkOverwrite = PropertiesUtil.getBooleanProperty("mjb.forcemjb.forceFanartTvOverwrite", "false");

        // XXX DEBUG
        artworkToken = "." + artworkTypeName;
        artworkFormat = "png";
        artworkValidate = Boolean.FALSE;

        if (PropertiesUtil.getBooleanProperty("scanner." + artworkTypeName + ".debug", "false")) {
            debugOutput();
            debugProperty("mjb.forcemjb.forceFanartTvOverwrite", artworkOverwrite, Boolean.FALSE);
        }
    }

    @Override
    public String getArtworkFilename(Movie movie) {
        return movie.getClearArtFilename();
    }

    @Override
    public String getArtworkUrl(Movie movie) {
        return movie.getClearArtURL();
    }

    @Override
    public boolean isDirtyArtwork(Movie movie) {
        return movie.isDirty(DirtyFlag.CLEARART);
    }

    @Override
    public void setArtworkFilename(Movie movie, String artworkFilename) {
        movie.setClearArtFilename(artworkFilename);
    }

    @Override
    public void setArtworkUrl(Movie movie, String artworkUrl) {
        movie.setClearArtURL(artworkUrl);
    }

    @Override
    public String scanLocalArtwork(Jukebox jukebox, Movie movie) {
        return super.scanLocalArtwork(jukebox, movie, artworkImagePlugin);
    }

    @Override
    public String scanOnlineArtwork(Movie movie) {
        logger.info(logMessage + "*** USE FANART.TV SCANNER TO GET THE ONLINE IMAGES");
        return Movie.UNKNOWN;
    }

    @Override
    public void setArtworkImagePlugin() {
        // Use the default image plugin
        setImagePlugin(null);
    }

    @Override
    public void setDirtyArtwork(Movie movie, boolean dirty) {
        movie.setDirty(DirtyFlag.CLEARART, dirty);
    }
}
