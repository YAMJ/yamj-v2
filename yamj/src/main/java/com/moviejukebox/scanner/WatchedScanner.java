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
package com.moviejukebox.scanner;

import static com.moviejukebox.tools.PropertiesUtil.TRUE;

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.model.enumerations.WatchedWithExtension;
import com.moviejukebox.model.enumerations.WatchedWithLocation;
import com.moviejukebox.plugin.*;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.TraktTvScanner;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.trakttv.model.*;

public class WatchedScanner {

    private static final Logger LOG = LoggerFactory.getLogger(WatchedScanner.class);
    private static final Collection<String> EXTENSIONS = Arrays.asList(PropertiesUtil.getProperty("mjb.watchedExtensions", "watched").toLowerCase().split(",;\\|"));
    private static final WatchedWithLocation LOCATION = WatchedWithLocation.fromString(PropertiesUtil.getProperty("mjb.watchedLocation", "withVideo"));
    private static final WatchedWithExtension WITH_EXTENSION = WatchedWithExtension.fromString(PropertiesUtil.getProperty("mjb.watched.withExtension", TRUE));
    private static final boolean WATCH_FILES = PropertiesUtil.getBooleanProperty("watched.scanner.enable", Boolean.TRUE);
    private static final boolean WATCH_TRAKTTV = PropertiesUtil.getBooleanProperty("watched.trakttv.enable", Boolean.TRUE);
    private static final TraktTvScanner TRAKT_TV_SCANNER = TraktTvScanner.getInstance();
    private static boolean warned = Boolean.FALSE;
    
    protected WatchedScanner() {
        throw new UnsupportedOperationException("Watched Scanner cannot be initialised");
    }

    /**
     * Calculate the watched state of a movie based on the files
     * {filename}.watched & {filename}.unwatched
     *
     * Always assumes that the file is unwatched if nothing is found.
     * 
     * Also TraktTV watched check can be done.
     *
     * @param jukebox
     * @param movie
     * @return
     */
    public static boolean checkWatched(Jukebox jukebox, Movie movie) {

        if (WATCH_FILES && !warned && (LOCATION == WatchedWithLocation.CUSTOM)) {
            LOG.warn("Custom file location not supported for watched scanner");
            warned = Boolean.TRUE;
        }

        TrackedShow trackedShow = null;
        TrackedMovie trackedMovie = null;
        if (WATCH_TRAKTTV ) {
            // PreLoad data from TraktTV
            if (movie.isTVShow()) {
                trackedShow = getMatchingShow(movie); 
            } else if (!movie.isExtra()) {
                trackedMovie = getMatchingMovie(movie);
            }
        }
        
        // assume no changes
        boolean returnStatus = Boolean.FALSE;
        // check if media file watched has changed
        boolean movieFileWatchChanged = Boolean.FALSE;
        // the number of watched files found        
        int fileWatchedCount = 0;

        File foundFile = null;
        boolean movieWatchedFile = Boolean.TRUE;

        for (MovieFile mf : movie.getFiles()) {
            // Check that the file pointer is valid
            if (mf.getFile() == null) {
                continue;
            }

            if (MovieJukebox.isJukeboxPreserve() && !mf.getFile().exists()) {
                fileWatchedCount++;
            } else {
                boolean fileWatched = Boolean.FALSE;
                long fileWatchedDate = mf.getWatchedDate();
                
                // check for watched/unwatched files on file system
                if (WATCH_FILES) {

                    String filename;
                    // BluRay stores the file differently to DVD and single files, so we need to process the path a little
                    if (movie.isBluray()) {
                        filename = new File(FileTools.getParentFolder(mf.getFile())).getName();
                    } else {
                        filename = mf.getFile().getName();
                    }
    
                    if (WITH_EXTENSION == WatchedWithExtension.EXTENSION || WITH_EXTENSION == WatchedWithExtension.BOTH || movie.isBluray()) {
                        if (LOCATION == WatchedWithLocation.WITHJUKEBOX) {
                            foundFile = FileTools.findFilenameInCache(filename, EXTENSIONS, jukebox, Boolean.TRUE);
                        } else {
                            foundFile = FileTools.findFilenameInCache(filename, EXTENSIONS, jukebox, Boolean.FALSE);
                        }
                    }
    
                    if (foundFile == null && (WITH_EXTENSION == WatchedWithExtension.NOEXTENSION || WITH_EXTENSION == WatchedWithExtension.BOTH) && !movie.isBluray()) {
                        // Remove the extension from the filename
                        filename = FilenameUtils.removeExtension(filename);
                        // Check again without the extension
                        if (LOCATION == WatchedWithLocation.WITHJUKEBOX) {
                            foundFile = FileTools.findFilenameInCache(filename, EXTENSIONS, jukebox, Boolean.TRUE);
                        } else {
                            foundFile = FileTools.findFilenameInCache(filename, EXTENSIONS, jukebox, Boolean.FALSE);
                        }
                    }
    
                    if (foundFile != null) {
                        fileWatchedCount++;
                        fileWatchedDate = new DateTime(foundFile.lastModified()).withMillisOfSecond(0).getMillis();
                        fileWatched = StringUtils.endsWithAny(foundFile.getName().toLowerCase(), EXTENSIONS.toArray(new String[0]));
                    }
                }

                // check for watched status from Trakt.TV
                if (WATCH_TRAKTTV) {
                    
                    // always increase file counter
                    fileWatchedCount++;
                    
                    long traktWatchedDate = 0;
                    if (movie.isTVShow()) {
                        // get watched date for episode
                        traktWatchedDate = watchedDate(trackedShow, mf);
                    } else if (!movie.isExtra()) {
                        // get watched date for movie
                        traktWatchedDate = watchedDate(trackedMovie);
                    }

                    // watched date only set if movie/episode has been watched
                    if (traktWatchedDate > 0) {
                        fileWatched = Boolean.TRUE;
                        fileWatchedDate = Math.max(traktWatchedDate, fileWatchedDate);
                    }
                }
                
                if (mf.setWatched(fileWatched, fileWatchedDate)) {
                    movieFileWatchChanged = Boolean.TRUE;
                }
            }

            // as soon as there is an unwatched file, the whole movie becomes unwatched
            movieWatchedFile = movieWatchedFile && mf.isWatched();
        }

        if (movieFileWatchChanged) {
            // set dirty flag if movie file watched status has changed
            movie.setDirty(DirtyFlag.WATCHED, Boolean.TRUE);
        }

        // change the watched status if:
        //  - we found at least 1 file and watched file has change
        //  - no files are found and the movie is watched
        if ((fileWatchedCount > 0 && movie.isWatchedFile() != movieWatchedFile) || (fileWatchedCount == 0 && movie.isWatchedFile())) {
            movie.setWatchedFile(movieWatchedFile);
            movie.setDirty(DirtyFlag.WATCHED, Boolean.TRUE);

            // Issue 1949 - Force the artwork to be overwritten (those that can have icons on them)
            movie.setDirty(DirtyFlag.POSTER, Boolean.TRUE);
            movie.setDirty(DirtyFlag.BANNER, Boolean.TRUE);

            returnStatus = Boolean.TRUE;
        }

        // build the final return status
        returnStatus |= movieFileWatchChanged;
        if (returnStatus) {
            LOG.debug("The video has one or more files that have changed status.");
        }
        return returnStatus;
    }

    private static boolean isMatchingMovie(TrackedMovie tracked, Movie movie) {
        final Ids ids = tracked.getMovie().getIds();
        final String traktId = StringUtils.trimToNull(movie.getId(TraktTvScanner.SCANNER_ID));
        final String tmdbId = StringUtils.trimToNull(movie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID));
        final String imdbId = StringUtils.trimToNull(movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));

        if (ids.trakt() != null && StringUtils.equals(traktId, ids.trakt().toString())) {
            return true;
        }
        if (ids.tmdb() != null && StringUtils.equals(tmdbId, ids.tmdb().toString())) {
            setTraktId(ids.trakt(), traktId, movie);
            return true;
        }
        if (ids.imdb() != null && StringUtils.equals(imdbId, ids.imdb())) {
            setTraktId(ids.trakt(), traktId, movie);
            return true;
        }
        return false;
    }

    private static boolean isMatchingShow(TrackedShow tracked, Movie movie) {
        final Ids ids = tracked.getShow().getIds();
        final String traktId = StringUtils.trimToNull(movie.getId(TraktTvScanner.SCANNER_ID));
        final String tvdbId = StringUtils.trimToNull(movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID));
        final String tvRageId = StringUtils.trimToNull(movie.getId(TVRagePlugin.TVRAGE_PLUGIN_ID));
        final String tmdbId = StringUtils.trimToNull(movie.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID));
        final String imdbId = StringUtils.trimToNull(movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        
        if (ids.trakt() != null && StringUtils.equals(traktId, ids.trakt().toString())) {
            return true;
        }
        if (ids.tvdb() != null && StringUtils.equals(tvdbId, ids.tvdb().toString())) {
            setTraktId(ids.trakt(), traktId, movie);
            return true;
        }
        if (ids.tvRage() != null && StringUtils.equals(tvRageId, ids.tvRage())) {
            setTraktId(ids.trakt(), traktId, movie);
            return true;
        }
        if (ids.tmdb() != null && StringUtils.equals(tmdbId, ids.tmdb().toString())) {
            setTraktId(ids.trakt(), traktId, movie);
            return true;
        }
        if (ids.imdb() != null && StringUtils.equals(imdbId, ids.imdb())) {
            setTraktId(ids.trakt(), traktId, movie);
            return true;
        }
        return false;
    }

    private static void setTraktId(Integer traktId, String presentTraktId, Movie movie) {
        if (presentTraktId == null && traktId != null && traktId.intValue() > 0) {
            movie.setId(TraktTvScanner.SCANNER_ID, traktId.toString());
        }
    }

    private static TrackedMovie getMatchingMovie(Movie movie) {
        for (TrackedMovie tracked : TRAKT_TV_SCANNER.getWatchedMovies()) {
            if (isMatchingMovie(tracked, movie)) {
                return tracked;
            }
        }
        return null;
    }

    private static TrackedShow getMatchingShow(Movie movie) {
        for (TrackedShow tracked : TRAKT_TV_SCANNER.getWatchedShows()) {
            if (isMatchingShow(tracked, movie)) {
                return tracked;
            }
        }
        return null;
    }

    private static TrackedSeason getMatchingSeason(TrackedShow show, MovieFile movieFile) {
        if (show != null) {
            for (TrackedSeason season : show.getSeasons()) {
                if (season.getNumber() != null && season.getNumber().intValue() == movieFile.getSeason()) {
                    return season;
                }
            }
        }
        return null;
    }

    private static long watchedDate(TrackedMovie movie) {
        if (movie == null) {
            // not watched if not found
            return 0;
        }
        return movie.getLastWatchedAt().withMillisOfSecond(0).getMillis();
    }

    private static long watchedDate(TrackedShow show, MovieFile movieFile) {
        TrackedSeason season = getMatchingSeason(show, movieFile);
        if (season == null) {
            // not watched if not found
            return 0;
        }

        // NOTE: all parts must be watched, so that the movie file can be set to watched
        long watchedDate = 0;
        for (int epNr = movieFile.getFirstPart(); epNr <= movieFile.getLastPart(); epNr++) {
            watchedDate = Math.max(watchedDate, watchedDate(season, epNr));
            if (watchedDate == 0) {
                // not all episodes in file are watched
                return 0;
            }
        }
        
        // not watched
        return 0;
    }

    private static long watchedDate(TrackedSeason season, int epNr) {
        for (TrackedEpisode episode : season.getEpisodes()) {
            if (episode.getNumber() != null && episode.getNumber().intValue() == epNr) {
                return episode.getLastWatchedAt().withMillisOfSecond(0).getMillis();
            }
        }
        
        // not watched
        return 0;
    }
}