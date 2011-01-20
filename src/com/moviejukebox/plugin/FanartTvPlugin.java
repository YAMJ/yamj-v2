package com.moviejukebox.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.moviejukebox.fanarttv.FanartTv;
import com.moviejukebox.fanarttv.model.FanartTvArtwork;
import com.moviejukebox.model.Artwork;
import com.moviejukebox.model.ArtworkType;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.tvrage.tools.StringTools;

public class FanartTvPlugin {
    private static final String THETVDB_PLUGIN_ID = "thetvdb";
    private FanartTv ft = new FanartTv();
    private List<FanartTvArtwork> ftArtwork = new ArrayList<FanartTvArtwork>();
    private static String LogMessage = "FanartTvPlugin: ";
    protected static Logger logger = Logger.getLogger("moviejukebox");
    
    public FanartTvPlugin() {
        // We need to set the proxy parameters if set.
        ft.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());

        // Set the timeout values
        ft.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }
    
    public boolean scan(Movie movie) {
        String tvdbidString = movie.getId(THETVDB_PLUGIN_ID);
        int tvdbid = 0;
        
        if (StringTools.isValidString(tvdbidString)) {
            try {
                tvdbid = Integer.parseInt(tvdbidString);
            } catch (Exception error) {
                tvdbid = 0;
            }
        }
        
        if (tvdbid > 0) {
            ftArtwork = ft.getArtwork(tvdbid);
            logger.fine(LogMessage + "Found " + ftArtwork.size() + " artwork items"); // XXX: DEBUG

            Artwork movieArtwork;
            
            for (FanartTvArtwork ft : ftArtwork) {
                movieArtwork = new Artwork();
                movieArtwork.setSourceSite("fanarttv");
                movieArtwork.setType(ArtworkType.fromString(ft.getType()));
                movieArtwork.setUrl(ft.getUrl());
                movie.addArtwork(movieArtwork);
            }
            
            return true;
        } else {
            logger.finer(LogMessage + "No artwork found for " + movie.getBaseName() + " with TVDBID: " + tvdbidString);
            return false;
        }
    }
}
