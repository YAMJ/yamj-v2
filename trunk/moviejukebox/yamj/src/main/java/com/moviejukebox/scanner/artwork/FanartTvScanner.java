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
import com.moviejukebox.plugin.FanartTvPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import com.moviejukebox.tools.StringTools;
import com.omertron.fanarttvapi.model.FTArtworkType;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Scanner for Fanart.TV artwork. Must be instantiated with the correct
 * Fanart.TV type
 *
 * @author stuart.boston
 */
public class FanartTvScanner extends ArtworkScanner {

//    private static final Logger logger = Logger.getLogger(FanartTvScanner.class);
    private static final FanartTvPlugin fanartTvPlugin = new FanartTvPlugin();

    public FanartTvScanner(ArtworkType fanartTvArtworkType) {
        super(fanartTvArtworkType);

        setOverwrite();
        setDownloadByType();

        if (PropertiesUtil.getBooleanProperty("scanner." + artworkTypeName + ".debug", FALSE)) {
            debugOutput();
        }
    }

    @Override
    public String scanLocalArtwork(Jukebox jukebox, Movie movie) {
        if (isSearchLocal()) {
            return super.scanLocalArtwork(jukebox, movie, artworkImagePlugin);
        } else {
            return Movie.UNKNOWN;
        }
    }

    @Override
    public String scanOnlineArtwork(Movie movie) {
        // Don't scan if we are not needed
        if ((isOverwrite() || StringTools.isNotValidString(getArtworkUrl(movie))) && isSearchOnline(movie)) {
            // Check the type of the video and whether it is required or not
            fanartTvPlugin.scan(movie, FTArtworkType.fromString(artworkType.toString()));
        }

        return getArtworkUrl(movie);
    }

    @Override
    public String getArtworkFilename(Movie movie) {
        if (artworkType == ArtworkType.ClearArt) {
            return movie.getClearArtFilename();
        } else if (artworkType == ArtworkType.ClearLogo) {
            return movie.getClearLogoFilename();
        } else if (artworkType == ArtworkType.TvThumb) {
            return movie.getTvThumbFilename();
        } else if (artworkType == ArtworkType.SeasonThumb) {
            return movie.getSeasonThumbFilename();
        } else if (artworkType == ArtworkType.MovieArt) {
            return movie.getClearArtFilename();
        } else if (artworkType == ArtworkType.MovieLogo) {
            return movie.getClearLogoFilename();
        } else if (artworkType == ArtworkType.MovieDisc) {
            return movie.getMovieDiscFilename();
        } else if (artworkType == ArtworkType.CharacterArt) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public void setArtworkFilename(Movie movie, String artworkFilename) {
        if (artworkType == ArtworkType.ClearArt) {
            movie.setClearArtFilename(artworkFilename);
        } else if (artworkType == ArtworkType.ClearLogo) {
            movie.setClearLogoFilename(artworkFilename);
        } else if (artworkType == ArtworkType.TvThumb) {
            movie.setTvThumbFilename(artworkFilename);
        } else if (artworkType == ArtworkType.SeasonThumb) {
            movie.setSeasonThumbFilename(artworkFilename);
        } else if (artworkType == ArtworkType.MovieArt) {
            movie.setClearArtFilename(artworkFilename);
        } else if (artworkType == ArtworkType.MovieLogo) {
            movie.setClearLogoFilename(artworkFilename);
        } else if (artworkType == ArtworkType.MovieDisc) {
            movie.setMovieDiscFilename(artworkFilename);
        } else if (artworkType == ArtworkType.CharacterArt) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public String getArtworkUrl(Movie movie) {
        if (artworkType == ArtworkType.ClearArt) {
            return movie.getClearArtURL();
        } else if (artworkType == ArtworkType.ClearLogo) {
            return movie.getClearLogoURL();
        } else if (artworkType == ArtworkType.TvThumb) {
            return movie.getTvThumbURL();
        } else if (artworkType == ArtworkType.SeasonThumb) {
            return movie.getSeasonThumbURL();
        } else if (artworkType == ArtworkType.MovieArt) {
            return movie.getClearArtURL();
        } else if (artworkType == ArtworkType.MovieLogo) {
            return movie.getClearLogoURL();
        } else if (artworkType == ArtworkType.MovieDisc) {
            return movie.getMovieDiscURL();
        } else if (artworkType == ArtworkType.CharacterArt) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public void setArtworkUrl(Movie movie, String artworkUrl) {
        if (artworkType == ArtworkType.ClearArt) {
            movie.setClearArtURL(artworkUrl);
        } else if (artworkType == ArtworkType.ClearLogo) {
            movie.setClearLogoURL(artworkUrl);
        } else if (artworkType == ArtworkType.TvThumb) {
            movie.setTvThumbURL(artworkUrl);
        } else if (artworkType == ArtworkType.SeasonThumb) {
            movie.setSeasonThumbURL(artworkUrl);
        } else if (artworkType == ArtworkType.MovieArt) {
            movie.setClearArtURL(artworkUrl);
        } else if (artworkType == ArtworkType.MovieLogo) {
            movie.setClearLogoURL(artworkUrl);
        } else if (artworkType == ArtworkType.MovieDisc) {
            movie.setMovieDiscURL(artworkUrl);
        } else if (artworkType == ArtworkType.CharacterArt) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public boolean isDirtyArtwork(Movie movie) {
        if (artworkType == ArtworkType.ClearArt) {
            movie.isDirty(DirtyFlag.CLEARART);
        } else if (artworkType == ArtworkType.ClearLogo) {
            movie.isDirty(DirtyFlag.CLEARLOGO);
        } else if (artworkType == ArtworkType.TvThumb) {
            movie.isDirty(DirtyFlag.TVTHUMB);
        } else if (artworkType == ArtworkType.SeasonThumb) {
            movie.isDirty(DirtyFlag.SEASONTHUMB);
        } else if (artworkType == ArtworkType.MovieArt) {
            movie.isDirty(DirtyFlag.CLEARART);
        } else if (artworkType == ArtworkType.MovieLogo) {
            movie.isDirty(DirtyFlag.CLEARLOGO);
        } else if (artworkType == ArtworkType.MovieDisc) {
            movie.isDirty(DirtyFlag.MOVIEDISC);
        } else if (artworkType == ArtworkType.CharacterArt) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
        return Boolean.FALSE;
    }

    @Override
    public void setDirtyArtwork(Movie movie, boolean dirty) {
        if (artworkType == ArtworkType.ClearArt) {
            movie.setDirty(DirtyFlag.CLEARART, dirty);
        } else if (artworkType == ArtworkType.ClearLogo) {
            movie.setDirty(DirtyFlag.CLEARLOGO, dirty);
        } else if (artworkType == ArtworkType.TvThumb) {
            movie.setDirty(DirtyFlag.TVTHUMB, dirty);
        } else if (artworkType == ArtworkType.SeasonThumb) {
            movie.setDirty(DirtyFlag.SEASONTHUMB, dirty);
        } else if (artworkType == ArtworkType.MovieArt) {
            movie.setDirty(DirtyFlag.CLEARART, dirty);
        } else if (artworkType == ArtworkType.MovieLogo) {
            movie.setDirty(DirtyFlag.CLEARLOGO, dirty);
        } else if (artworkType == ArtworkType.MovieDisc) {
            movie.setDirty(DirtyFlag.MOVIEDISC, dirty);
        } else if (artworkType == ArtworkType.CharacterArt) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    /**
     * Determine the overwrite property from the artwork type
     *
     * @return
     */
    private boolean setOverwrite() {
        String propName = "mjb.force" + StringUtils.capitalize(artworkTypeName) + "Overwrite";
        artworkOverwrite = PropertiesUtil.getBooleanProperty(propName, FALSE);
//        logger.debug(LOG_MESSAGE + propName + "=" + artworkOverwrite);
        return artworkOverwrite;
    }

    /**
     * Optimise the downloads based on the artwork type.
     */
    private void setDownloadByType() {
        if (artworkType == ArtworkType.ClearArt) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.ClearLogo) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.TvThumb) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.SeasonThumb) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.MovieArt) {
            // TV download is not supported for this type
            artworkDownloadTv = Boolean.FALSE;
        } else if (artworkType == ArtworkType.MovieLogo) {
            // TV download is not supported for this type
            artworkDownloadTv = Boolean.FALSE;
        } else if (artworkType == ArtworkType.MovieDisc) {
            // TV download is not supported for this type
            artworkDownloadTv = Boolean.FALSE;
        } else if (artworkType == ArtworkType.CharacterArt) {
            artworkSearchLocal = Boolean.FALSE;
            artworkOverwrite = Boolean.FALSE;
            artworkDownloadMovie = Boolean.FALSE;
            artworkDownloadTv = Boolean.FALSE;
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            artworkSearchLocal = Boolean.FALSE;
            artworkOverwrite = Boolean.FALSE;
            artworkDownloadMovie = Boolean.FALSE;
            artworkDownloadTv = Boolean.FALSE;
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public void setArtworkImagePlugin() {
        // Use the default image plugin
        setImagePlugin(null);
    }
}
