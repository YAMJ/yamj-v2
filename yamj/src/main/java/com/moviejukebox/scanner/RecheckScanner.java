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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Artwork.ArtworkType;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.ArtworkScanner;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import com.moviejukebox.tools.SystemTools;
import java.util.Date;
import java.util.EnumSet;
import org.apache.log4j.Logger;

/**
 * This function will validate the current movie object and return true if the
 * movie needs to be re-scanned.
 *
 * @author Stuart
 */
public class RecheckScanner {

    private static final Logger logger = Logger.getLogger(RecheckScanner.class);
    private static final String logMessage = "RecheckScanner: ";
    /*
     * Recheck variables
     */
    private static final boolean recheckXML = PropertiesUtil.getBooleanProperty("mjb.recheck.XML", "true");
    private static final int recheckMax = PropertiesUtil.getIntProperty("mjb.recheck.Max", "50");
    private static final boolean recheckVersion = PropertiesUtil.getBooleanProperty("mjb.recheck.Version", "true");
    private static final int recheckDays = PropertiesUtil.getIntProperty("mjb.recheck.Days", "30");
    private static final int recheckMinDays = PropertiesUtil.getIntProperty("mjb.recheck.minDays", "7");
    private static final int recheckRevision = PropertiesUtil.getIntProperty("mjb.recheck.Revision", "25");
    private static final boolean recheckUnknown = PropertiesUtil.getBooleanProperty("mjb.recheck.Unknown", "true");
    // How many rechecks have been performed
    private static int recheckCount = 0;

    /*
     * Property values
     */
    private static final boolean fanartMovieDownload = PropertiesUtil.getBooleanProperty("fanart.movie.download", "false");
    private static final boolean fanartTvDownload = PropertiesUtil.getBooleanProperty("fanart.tv.download", "false");
    private static final boolean videoimageDownload = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
    private static final boolean bannerDownload = PropertiesUtil.getBooleanProperty("mjb.includeWideBanners", "false");
    private static final boolean includeEpisodeRating = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", "false");
    private static final boolean includePeople = PropertiesUtil.getBooleanProperty("mjb.people", "false");
    private static final EnumSet<ArtworkType> artworkRequired = ArtworkScanner.getRequiredArtworkTypes();

    public static boolean scan(Movie movie) {
        if (!recheckXML) {
            return false;
        }

        // Skip Extras (Trailers, etc)
        if (movie.isExtra()) {
            return false;
        }

        logger.debug(logMessage + "Checking " + movie.getBaseName());

        /*
         * Always perform these checks, regardless of the recheckCount
         */

        // Check for the version of YAMJ that wrote the XML file vs the current version
        //System.out.println("- mjbVersion : " + movie.getMjbVersion() + " (" + movie.getCurrentMjbVersion() + ")");
        if (recheckVersion && !movie.getMjbVersion().equalsIgnoreCase(SystemTools.getVersion())) {
            logger.debug(logMessage + movie.getBaseName() + " XML is from a previous version, will rescan");
            return true;
        }

        if (includePeople && movie.getPeople().isEmpty()) {
            logger.debug(logMessage + movie.getBaseName() + " is missing people data, will rescan");
            return true;
        }

        /*
         * End of permanent checks
         */

        if (recheckCount >= recheckMax) {
            // We are over the recheck maximum, so we won't recheck again this run
            return false;
        } else if (recheckCount == recheckMax) {
            logger.debug(logMessage + "Threshold of " + recheckMax + " rechecked movies reached. No more will be checked until the next run.");
            recheckCount++; // By incrementing this variable we will only display this message once.
            return false;
        }

        Date currentDate = new Date();
        long dateDiff = (currentDate.getTime() - movie.getMjbGenerationDate().toDate().getTime()) / (1000 * 60 * 60 * 24);

        // Check the date the XML file was last written to and skip if it's less than minDays
        if ((recheckMinDays > 0) && (dateDiff <= recheckMinDays)) {
            logger.debug(logMessage + movie.getBaseName() + " - XML is " + dateDiff + " days old, less than recheckMinDays (" + recheckMinDays + "), not checking.");
            return false;
        }

        // Check the date the XML file was written vs the current date
        if ((recheckDays > 0) && (dateDiff > recheckDays)) {
            logger.debug(logMessage + movie.getBaseName() + " XML is " + dateDiff + " days old, will rescan");
            recheckCount++;
            return true;
        }

        // If we don't recheck the version, we don't want to recheck the revision either
        if (recheckVersion) {
            // Check the revision of YAMJ that wrote the XML file vs the current revisions
            //System.out.println("- mjbRevision: " + movie.getMjbRevision() + " (" + movie.getCurrentMjbRevision() + ")");
            //System.out.println("- Difference : " + (Integer.parseInt(movie.getCurrentMjbRevision()) - Integer.parseInt(movie.getMjbRevision())) );
            String currentRevision = SystemTools.getRevision();
            String movieMjbRevision = movie.getMjbRevision();
            int revDiff = Integer.parseInt(isNotValidString(currentRevision) ? "0" : currentRevision) - Integer.parseInt(isNotValidString(movieMjbRevision) ? "0" : movieMjbRevision);
            if (revDiff > recheckRevision) {
                logger.debug(logMessage + movie.getBaseName() + " XML is " + revDiff + " revisions old (" + recheckRevision + " maximum), will rescan");
                recheckCount++;
                return true;
            }
        }

        // Check for "UNKNOWN" values in the XML
        if (recheckUnknown) {
            if (isNotValidString(movie.getTitle()) && isNotValidString(movie.getYear())) {
                logger.debug(logMessage + movie.getBaseName() + " XML is missing the title, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getPlot())) {
                logger.debug(logMessage + movie.getBaseName() + " XML is missing plot, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getYear())) {
                logger.debug(logMessage + movie.getBaseName() + " XML is missing year, will rescan");
                recheckCount++;
                return true;
            }

            if (movie.getGenres().isEmpty()) {
                logger.debug(logMessage + movie.getBaseName() + " XML is missing genres, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getPosterURL())) {
                logger.debug(logMessage + movie.getBaseName() + " is missing poster, will rescan");
                recheckCount++;
                return true;
            }

            if (isNotValidString(movie.getFanartURL())) {
                if ((fanartMovieDownload && !movie.isTVShow()) || (fanartTvDownload && movie.isTVShow())) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing fanart, will rescan");
                    recheckCount++;
                    return true;
                }
            }

            // Fanart.TV checking
            if (movie.isTVShow()) {
                if (isNotValidString(movie.getClearArtURL()) && artworkRequired.contains(ArtworkType.ClearArt)) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing ClearArt, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getClearLogoURL()) && artworkRequired.contains(ArtworkType.ClearLogo)) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing ClearLogo, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getSeasonThumbURL()) && artworkRequired.contains(ArtworkType.SeasonThumb)) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing SeasonThumb, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getTvThumbURL()) && artworkRequired.contains(ArtworkType.TvThumb)) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing TvThumb, will rescan");
                    recheckCount++;
                    return true;
                }
            } else {
                if (isNotValidString(movie.getClearArtURL()) && artworkRequired.contains(ArtworkType.MovieArt)) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing MovieArt, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getClearLogoURL()) && artworkRequired.contains(ArtworkType.MovieLogo)) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing MovieLogo, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getMovieDiscURL()) && artworkRequired.contains(ArtworkType.MovieDisc)) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing MovieDisc, will rescan");
                    recheckCount++;
                    return true;
                }
            }

            // Only get ratings if the rating list is null or empty - We assume it's OK to have a -1 rating if there are entries in the array
            if (movie.getRatings() == null || movie.getRatings().isEmpty()) {
                logger.debug(logMessage + movie.getBaseName() + " is missing rating, will rescan");
                recheckCount++;
                return true;
            }

            if (movie.isTVShow()) {
                boolean recheckEpisodePlots = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", "false");

                if (bannerDownload && isNotValidString(movie.getBannerURL())) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing banner artwork, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getShowStatus())) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing show status, will rescan");
                    recheckCount++;
                    return true;
                }

                if (isNotValidString(movie.getReleaseDate())) {
                    logger.debug(logMessage + movie.getBaseName() + " is missing show release date, will rescan");
                    recheckCount++;
                    return true;
                }

                // scan the TV episodes
                for (MovieFile mf : movie.getMovieFiles()) {
                    if (isNotValidString(mf.getTitle())) {
                        logger.debug(logMessage + movie.getBaseName() + " - Part " + mf.getFirstPart() + " XML is missing Title, will rescan");
                        mf.setNewFile(true); // This forces the episodes to be rechecked
                        recheckCount++;
                        return true;
                    }

                    if (recheckEpisodePlots || videoimageDownload) {
                        for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                            if (recheckEpisodePlots && isNotValidString(mf.getPlot(part))) {
                                logger.debug(logMessage + movie.getBaseName() + " - Part " + part + " XML is missing TV plot, will rescan");
                                mf.setNewFile(true); // This forces the episodes to be rechecked
                                recheckCount++;
                                return true;
                            } // plots

                            if (videoimageDownload && isNotValidString(mf.getVideoImageURL(part))) {
                                logger.debug(logMessage + movie.getBaseName() + " - Part " + part + " XML is missing TV video image, will rescan");
                                mf.setNewFile(true); // This forces the episodes to be rechecked
                                recheckCount++;
                                return true;
                            } // videoimages
                        } // moviefile parts loop
                    } // if


                    for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                        if (isNotValidString(mf.getFirstAired(part))) {
                            logger.debug(logMessage + movie.getBaseName() + " - Part " + part + " XML is missing TV first aired date, will rescan");
                            mf.setNewFile(true); // This forces the episodes to be rechecked
                            recheckCount++;
                            return true;
                        }
                    }

                    for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                        if (includeEpisodeRating && isNotValidString(mf.getRating(part))) {
                            logger.debug(logMessage + movie.getBaseName() + " - Part " + part + " XML is missing TV rating, will rescan");
                            mf.setNewFile(true); // This forces the episodes to be rechecked
                            recheckCount++;
                            return true;
                        }
                    }
                } // moviefiles loop
            } // isTVShow
        }
        return false;
    }
}
