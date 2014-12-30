/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.ArtworkScanner;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import com.moviejukebox.tools.SystemTools;
import java.util.Date;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This function will validate the current movie object and return true if the movie needs to be re-scanned.
 *
 * @author Stuart
 */
public final class RecheckScanner {

    private static final Logger LOG = LoggerFactory.getLogger(RecheckScanner.class);
    private static final String LOG_MESSAGE = "RecheckScanner: ";
    /*
     * Recheck variables
     */
    private static final int RECHECK_MAX = PropertiesUtil.getIntProperty("mjb.recheck.Max", 50);
    private static final boolean RECHECK_XML = PropertiesUtil.getBooleanProperty("mjb.recheck.XML", Boolean.TRUE);
    private static final boolean RECHECK_VERSION = PropertiesUtil.getBooleanProperty("mjb.recheck.Version", Boolean.TRUE);
    private static final int RECHECK_DAYS = PropertiesUtil.getIntProperty("mjb.recheck.Days", 30);
    private static final int RECHECK_MIN_DAYS = PropertiesUtil.getIntProperty("mjb.recheck.minDays", 7);
    private static final int RECHECK_REVISION = PropertiesUtil.getIntProperty("mjb.recheck.Revision", 25);
    private static final boolean RECHECK_UNKNOWN = PropertiesUtil.getBooleanProperty("mjb.recheck.Unknown", Boolean.TRUE);
    private static final boolean RECHECK_EPISODE_PLOTS = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", Boolean.FALSE);
    // How many rechecks have been performed
    private static int recheckCount = 0;

    /*
     * Property values
     */
    private static final boolean FANART_MOVIE_DOWNLOAD = PropertiesUtil.getBooleanProperty("fanart.movie.download", Boolean.FALSE);
    private static final boolean FANART_TV_DOWNLOAD = PropertiesUtil.getBooleanProperty("fanart.tv.download", Boolean.FALSE);
    private static final boolean VIDEOIMAGE_DOWNLOAD = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", Boolean.FALSE);
    private static final boolean BANNER_DOWNLOAD = PropertiesUtil.getBooleanProperty("mjb.includeWideBanners", Boolean.FALSE);
    private static final boolean INCLUDE_EPISODE_RATING = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", Boolean.FALSE);
    private static final boolean INCLUDE_PEOPLE = PropertiesUtil.getBooleanProperty("mjb.people", Boolean.FALSE);
    private static final Set<ArtworkType> ARTWORK_REQUIRED = ArtworkScanner.getRequiredArtworkTypes();
    private static final String TV_PART_TEXT = " - Part ";

    private RecheckScanner() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    public static boolean scan(Movie movie) {
        // Do we need to recheck? Or is this an extra?
        if (!RECHECK_XML || movie.isExtra()) {
            return false;
        }

        LOG.debug(LOG_MESSAGE + "Checking " + movie.getBaseName());

        // Always perform these checks, regardless of the recheckCount
        if (recheckAlways(movie)) {
            return true;
        }

        if (recheckCount >= RECHECK_MAX) {
            // We are over the recheck maximum, so we won't recheck again this run
            return false;
        } else if (recheckCount == RECHECK_MAX) {
            LOG.debug(LOG_MESSAGE + "Threshold of " + RECHECK_MAX + " rechecked movies reached. No more will be checked until the next run.");
            recheckCount++; // By incrementing this variable we will only display this message once.
            return false;
        }

        Date currentDate = new Date();
        long dateDiff = (currentDate.getTime() - movie.getMjbGenerationDate().toDate().getTime()) / (1000 * 60 * 60 * 24);

        // Check the date the XML file was last written to and skip if it's less than minDays
        if ((RECHECK_MIN_DAYS > 0) && (dateDiff <= RECHECK_MIN_DAYS)) {
            LOG.debug(LOG_MESSAGE + movie.getBaseName() + " - XML is " + dateDiff + " days old, less than recheckMinDays (" + RECHECK_MIN_DAYS + "), not checking.");
            return false;
        }

        // Check the date the XML file was written vs the current date
        if ((RECHECK_DAYS > 0) && (dateDiff > RECHECK_DAYS)) {
            LOG.debug(LOG_MESSAGE + movie.getBaseName() + " XML is " + dateDiff + " days old, will rescan");
            recheckCount++;
            return true;
        }

        // If we don't recheck the version, we don't want to recheck the revision either
        if (RECHECK_VERSION) {
            // Check the revision of YAMJ that wrote the XML file vs the current revisions
            //System.out.println("- mjbRevision: " + movie.getMjbRevision() + " (" + movie.getCurrentMjbRevision() + ")");
            //System.out.println("- Difference : " + (Integer.parseInt(movie.getCurrentMjbRevision()) - Integer.parseInt(movie.getMjbRevision())) );
            String currentRevision = SystemTools.getRevision();
            String movieMjbRevision = movie.getMjbRevision();
            int revDiff = Integer.parseInt(isNotValidString(currentRevision) ? "0" : currentRevision) - Integer.parseInt(isNotValidString(movieMjbRevision) ? "0" : movieMjbRevision);
            if (revDiff > RECHECK_REVISION) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " XML is " + revDiff + " revisions old (" + RECHECK_REVISION + " maximum), will rescan");
                recheckCount++;
                return true;
            }
        }

        // Check for "UNKNOWN" values in the XML
        if (RECHECK_UNKNOWN) {
            if (isNotValidString(movie.getTitle()) && isNotValidString(movie.getYear())) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " XML is missing the title, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getPlot())) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " XML is missing plot, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getYear())) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " XML is missing year, will rescan");
                recheckCount++;
                return true;
            }

            if (movie.getGenres().isEmpty()) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " XML is missing genres, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getPosterURL())) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing poster, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getFanartURL()) && ((FANART_MOVIE_DOWNLOAD && !movie.isTVShow()) || (FANART_TV_DOWNLOAD && movie.isTVShow()))) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing fanart, will rescan");
                recheckCount++;
                return true;
            }

            // Check the FanartTV URLs
            if (fanartTvCheck(movie)) {
                return true;
            }

            // Only get ratings if the rating list is null or empty - We assume it's OK to have a -1 rating if there are entries in the array
            if (movie.getRatings() == null || movie.getRatings().isEmpty()) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing rating, will rescan");
                recheckCount++;
                return true;
            }

            if (movie.isTVShow()) {
                if (BANNER_DOWNLOAD && isNotValidString(movie.getBannerURL())) {
                    LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing banner artwork, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getShowStatus())) {
                    LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing show status, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getReleaseDate())) {
                    LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing show release date, will rescan");
                    recheckCount++;
                    return true;
                }

                // Check the TV Episodes
                if (tvEpisodesCheck(movie)) {
                    return true;
                }
            } // isTVShow
        }
        return false;
    }

    /**
     * Always perform these checks regardless of recheck count.
     *
     * @param movie
     * @return
     */
    private static boolean recheckAlways(Movie movie) {
        // Check for the version of YAMJ that wrote the XML file vs the current version
        //System.out.println("- mjbVersion : " + movie.getMjbVersion() + " (" + movie.getCurrentMjbVersion() + ")");
        if (RECHECK_VERSION && !movie.getMjbVersion().equalsIgnoreCase(SystemTools.getVersion())) {
            LOG.debug(LOG_MESSAGE + movie.getBaseName() + " XML is from a previous version, will rescan");
            return true;
        }

        if (INCLUDE_PEOPLE && movie.getPeople().isEmpty()) {
            LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing people data, will rescan");
            return true;
        }
        return false;
    }

    /**
     * FANART.TV checking
     *
     * @param movie
     * @return
     */
    private static boolean fanartTvCheck(Movie movie) {
        if (movie.isTVShow()) {
            if (isNotValidString(movie.getClearArtURL()) && ARTWORK_REQUIRED.contains(ArtworkType.CLEARART)) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing ClearArt, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getClearLogoURL()) && ARTWORK_REQUIRED.contains(ArtworkType.CLEARLOGO)) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing ClearLogo, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getSeasonThumbURL()) && ARTWORK_REQUIRED.contains(ArtworkType.SEASONTHUMB)) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing SeasonThumb, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getTvThumbURL()) && ARTWORK_REQUIRED.contains(ArtworkType.TVTHUMB)) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing TvThumb, will rescan");
                recheckCount++;
                return true;
            }
        } else {
            if (isNotValidString(movie.getClearArtURL()) && ARTWORK_REQUIRED.contains(ArtworkType.MOVIEART)) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing MovieArt, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getClearLogoURL()) && ARTWORK_REQUIRED.contains(ArtworkType.MOVIELOGO)) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing MovieLogo, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getMovieDiscURL()) && ARTWORK_REQUIRED.contains(ArtworkType.MOVIEDISC)) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + " is missing MovieDisc, will rescan");
                recheckCount++;
                return true;
            }
        }
        return false;
    }

    /**
     * Scan the TV episodes
     *
     * @param movie
     * @return
     */
    private static boolean tvEpisodesCheck(Movie movie) {
        for (MovieFile mf : movie.getMovieFiles()) {
            if (isNotValidString(mf.getTitle())) {
                LOG.debug(LOG_MESSAGE + movie.getBaseName() + TV_PART_TEXT + mf.getFirstPart() + " XML is missing Title, will rescan");
                mf.setNewFile(true); // This forces the episodes to be rechecked
                recheckCount++;
                return true;
            }

            if (RECHECK_EPISODE_PLOTS || VIDEOIMAGE_DOWNLOAD) {
                for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                    if (RECHECK_EPISODE_PLOTS && isNotValidString(mf.getPlot(part))) {
                        LOG.debug(LOG_MESSAGE + movie.getBaseName() + TV_PART_TEXT + part + " XML is missing TV plot, will rescan");
                        mf.setNewFile(true); // This forces the episodes to be rechecked
                        recheckCount++;
                        return true;
                    } // plots

                    if (VIDEOIMAGE_DOWNLOAD && isNotValidString(mf.getVideoImageURL(part))) {
                        LOG.debug(LOG_MESSAGE + movie.getBaseName() + TV_PART_TEXT + part + " XML is missing TV video image, will rescan");
                        mf.setNewFile(true); // This forces the episodes to be rechecked
                        recheckCount++;
                        return true;
                    } // videoimages
                } // moviefile parts loop
            } // if


            for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                if (isNotValidString(mf.getFirstAired(part))) {
                    LOG.debug(LOG_MESSAGE + movie.getBaseName() + TV_PART_TEXT + part + " XML is missing TV first aired date, will rescan");
                    mf.setNewFile(true); // This forces the episodes to be rechecked
                    recheckCount++;
                    return true;
                }
            }

            for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                if (INCLUDE_EPISODE_RATING && isNotValidString(mf.getRating(part))) {
                    LOG.debug(LOG_MESSAGE + movie.getBaseName() + TV_PART_TEXT + part + " XML is missing TV rating, will rescan");
                    mf.setNewFile(true); // This forces the episodes to be rechecked
                    recheckCount++;
                    return true;
                }
            }
        } // moviefiles loop

        return false;
    }
}
