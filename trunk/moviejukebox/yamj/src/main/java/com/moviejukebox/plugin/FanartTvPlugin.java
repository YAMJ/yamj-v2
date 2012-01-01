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
package com.moviejukebox.plugin;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.moviejukebox.fanarttv.FanartTv;
import com.moviejukebox.fanarttv.model.FanartTvArtwork;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Artwork.Artwork;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;

public class FanartTvPlugin {
    private FanartTv ft = new FanartTv();
    private List<FanartTvArtwork> ftArtwork = new ArrayList<FanartTvArtwork>();
    private static String logMessage = "FanartTvPlugin: ";
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private static final String webhost = "fanart.tv";

    public FanartTvPlugin() {
        // We need to set the proxy parameters if set.
        ft.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        ft.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }
    
    /**
     * Scan and return all artwork types (Defaults type to null)
     * @param movie
     * @return
     */
    public boolean scan(Movie movie) {
        return scan(movie, null);
    }
    
    public boolean scan(Movie movie, String artworkType) {
        String tvdbidString = movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID);
        int tvdbid = 0;
        
        if (StringTools.isValidString(tvdbidString)) {
            try {
                tvdbid = Integer.parseInt(tvdbidString);
            } catch (Exception error) {
                tvdbid = 0;
            }
        }
        
        if (tvdbid > 0) {
            ftArtwork = getFanartTvArtwork(tvdbid, artworkType);
            logger.debug(logMessage + "Found " + ftArtwork.size() + (StringTools.isValidString(artworkType)? artworkType : "") + " artwork items");

            Artwork movieArtwork;
            
            for (FanartTvArtwork ftSingle : ftArtwork) {
                movieArtwork = new Artwork();
                movieArtwork.setSourceSite("fanarttv");
                movieArtwork.setType(ArtworkType.fromString(ftSingle.getType()));
                movieArtwork.setUrl(ftSingle.getUrl());
                movie.addArtwork(movieArtwork);
            }
            
            return true;
        } else {
            logger.debug(logMessage + "No artwork found for " + movie.getBaseName() + " with TVDBID: " + tvdbidString);
            return false;
        }
    }
    
    public List<FanartTvArtwork> getFanartTvArtwork(int tvdbid, String artworkType) {
        ThreadExecutor.enterIO(webhost);
        try {
            return ft.getArtwork(tvdbid, artworkType, null);
        } finally {
            ThreadExecutor.leaveIO();
        }
    }
}
