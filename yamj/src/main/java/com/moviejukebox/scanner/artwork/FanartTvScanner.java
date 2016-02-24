/*
 *      Copyright (c) 2004-2016 YAMJ Members
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
package com.moviejukebox.scanner.artwork;

import org.apache.commons.lang3.StringUtils;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.plugin.FanartTvPlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.omertron.fanarttvapi.enumeration.FTArtworkType;

/**
 * Scanner for FANART.TV artwork. Must be instantiated with the correct
 FANART.TV type
 *
 * @author stuart.boston
 */
public class FanartTvScanner extends ArtworkScanner {

    private static final FanartTvPlugin FT_PLUGIN = new FanartTvPlugin();

    public FanartTvScanner(ArtworkType fanartTvArtworkType) {
        super(fanartTvArtworkType);

        setOverwrite();
        setDownloadByType();

        if (PropertiesUtil.getBooleanProperty("scanner." + artworkTypeName + ".debug", Boolean.FALSE)) {
            debugOutput();
        }
    }

    @Override
    public String scanLocalArtwork(Jukebox jukebox, Movie movie) {
        if (isSearchLocal()) {
            return super.scanLocalArtwork(jukebox, movie, artworkImagePlugin);
        }
        return Movie.UNKNOWN;
    }

    @Override
    public String scanOnlineArtwork(Movie movie) {
        // Don't scan if we are not needed
        if ((isOverwrite() || StringTools.isNotValidString(getArtworkUrl(movie))) && isSearchOnline(movie)) {
            // Check the type of the video and whether it is required or not
            FT_PLUGIN.scan(movie, FTArtworkType.fromString(artworkType.toString()));
        }

        return getArtworkUrl(movie);
    }

    @Override
    public String getOriginalFilename(Movie movie) {
        if (artworkType == ArtworkType.CLEARART) {
            return movie.getClearArtFilename();
        } else if (artworkType == ArtworkType.CLEARLOGO) {
            return movie.getClearLogoFilename();
        } else if (artworkType == ArtworkType.TVTHUMB) {
            return movie.getTvThumbFilename();
        } else if (artworkType == ArtworkType.SEASONTHUMB) {
            return movie.getSeasonThumbFilename();
        } else if (artworkType == ArtworkType.MOVIEART) {
            return movie.getClearArtFilename();
        } else if (artworkType == ArtworkType.MOVIELOGO) {
            return movie.getClearLogoFilename();
        } else if (artworkType == ArtworkType.MOVIEDISC) {
            return movie.getMovieDiscFilename();
        } else if (artworkType == ArtworkType.CHARACTERART) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public void setOriginalFilename(Movie movie, String artworkFilename) {
        if (artworkType == ArtworkType.CLEARART) {
            movie.setClearArtFilename(artworkFilename);
        } else if (artworkType == ArtworkType.CLEARLOGO) {
            movie.setClearLogoFilename(artworkFilename);
        } else if (artworkType == ArtworkType.TVTHUMB) {
            movie.setTvThumbFilename(artworkFilename);
        } else if (artworkType == ArtworkType.SEASONTHUMB) {
            movie.setSeasonThumbFilename(artworkFilename);
        } else if (artworkType == ArtworkType.MOVIEART) {
            movie.setClearArtFilename(artworkFilename);
        } else if (artworkType == ArtworkType.MOVIELOGO) {
            movie.setClearLogoFilename(artworkFilename);
        } else if (artworkType == ArtworkType.MOVIEDISC) {
            movie.setMovieDiscFilename(artworkFilename);
        } else if (artworkType == ArtworkType.CHARACTERART) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public String getArtworkUrl(Movie movie) {
        if (artworkType == ArtworkType.CLEARART) {
            return movie.getClearArtURL();
        } else if (artworkType == ArtworkType.CLEARLOGO) {
            return movie.getClearLogoURL();
        } else if (artworkType == ArtworkType.TVTHUMB) {
            return movie.getTvThumbURL();
        } else if (artworkType == ArtworkType.SEASONTHUMB) {
            return movie.getSeasonThumbURL();
        } else if (artworkType == ArtworkType.MOVIEART) {
            return movie.getClearArtURL();
        } else if (artworkType == ArtworkType.MOVIELOGO) {
            return movie.getClearLogoURL();
        } else if (artworkType == ArtworkType.MOVIEDISC) {
            return movie.getMovieDiscURL();
        } else if (artworkType == ArtworkType.CHARACTERART) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public void setArtworkUrl(Movie movie, String artworkUrl) {
        if (artworkType == ArtworkType.CLEARART) {
            movie.setClearArtURL(artworkUrl);
        } else if (artworkType == ArtworkType.CLEARLOGO) {
            movie.setClearLogoURL(artworkUrl);
        } else if (artworkType == ArtworkType.TVTHUMB) {
            movie.setTvThumbURL(artworkUrl);
        } else if (artworkType == ArtworkType.SEASONTHUMB) {
            movie.setSeasonThumbURL(artworkUrl);
        } else if (artworkType == ArtworkType.MOVIEART) {
            movie.setClearArtURL(artworkUrl);
        } else if (artworkType == ArtworkType.MOVIELOGO) {
            movie.setClearLogoURL(artworkUrl);
        } else if (artworkType == ArtworkType.MOVIEDISC) {
            movie.setMovieDiscURL(artworkUrl);
        } else if (artworkType == ArtworkType.CHARACTERART) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
    }

    @Override
    public boolean isDirtyArtwork(Movie movie) {
        if (artworkType == ArtworkType.CLEARART) {
            movie.isDirty(DirtyFlag.CLEARART);
        } else if (artworkType == ArtworkType.CLEARLOGO) {
            movie.isDirty(DirtyFlag.CLEARLOGO);
        } else if (artworkType == ArtworkType.TVTHUMB) {
            movie.isDirty(DirtyFlag.TVTHUMB);
        } else if (artworkType == ArtworkType.SEASONTHUMB) {
            movie.isDirty(DirtyFlag.SEASONTHUMB);
        } else if (artworkType == ArtworkType.MOVIEART) {
            movie.isDirty(DirtyFlag.CLEARART);
        } else if (artworkType == ArtworkType.MOVIELOGO) {
            movie.isDirty(DirtyFlag.CLEARLOGO);
        } else if (artworkType == ArtworkType.MOVIEDISC) {
            movie.isDirty(DirtyFlag.MOVIEDISC);
        } else if (artworkType == ArtworkType.CHARACTERART) {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        } else {
            throw new IllegalArgumentException(artworkTypeName + " is not supported by this scanner");
        }
        return Boolean.FALSE;
    }

    @Override
    public void setDirtyArtwork(Movie movie, boolean dirty) {
        if (artworkType == ArtworkType.CLEARART) {
            movie.setDirty(DirtyFlag.CLEARART, dirty);
        } else if (artworkType == ArtworkType.CLEARLOGO) {
            movie.setDirty(DirtyFlag.CLEARLOGO, dirty);
        } else if (artworkType == ArtworkType.TVTHUMB) {
            movie.setDirty(DirtyFlag.TVTHUMB, dirty);
        } else if (artworkType == ArtworkType.SEASONTHUMB) {
            movie.setDirty(DirtyFlag.SEASONTHUMB, dirty);
        } else if (artworkType == ArtworkType.MOVIEART) {
            movie.setDirty(DirtyFlag.CLEARART, dirty);
        } else if (artworkType == ArtworkType.MOVIELOGO) {
            movie.setDirty(DirtyFlag.CLEARLOGO, dirty);
        } else if (artworkType == ArtworkType.MOVIEDISC) {
            movie.setDirty(DirtyFlag.MOVIEDISC, dirty);
        } else if (artworkType == ArtworkType.CHARACTERART) {
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
        artworkOverwrite = PropertiesUtil.getBooleanProperty(propName, Boolean.FALSE);
//        logger.debug(logMessage + propName + "=" + artworkOverwrite);
        return artworkOverwrite;
    }

    /**
     * Optimise the downloads based on the artwork type.
     */
    private void setDownloadByType() {
        if (artworkType == ArtworkType.CLEARART) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.CLEARLOGO) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.TVTHUMB) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.SEASONTHUMB) {
            // Movie download is not supported for this type
            artworkDownloadMovie = Boolean.FALSE;
        } else if (artworkType == ArtworkType.MOVIEART) {
            // TV download is not supported for this type
            artworkDownloadTv = Boolean.FALSE;
        } else if (artworkType == ArtworkType.MOVIELOGO) {
            // TV download is not supported for this type
            artworkDownloadTv = Boolean.FALSE;
        } else if (artworkType == ArtworkType.MOVIEDISC) {
            // TV download is not supported for this type
            artworkDownloadTv = Boolean.FALSE;
        } else if (artworkType == ArtworkType.CHARACTERART) {
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

    @Override
    public String getJukeboxFilename(Movie movie) {
        return Movie.UNKNOWN;
    }

    @Override
    public void setJukeboxFilename(Movie movie, String artworkFilename) {
        // Not immplemented
    }
}
