/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.tools;

import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.MovieFilenameScanner;
import java.util.*;
import org.apache.log4j.Logger;

public final class SubtitleTools {

    private static final Logger LOG = Logger.getLogger(SubtitleTools.class);
    private static final String LOG_MESSAGE = "SubtitleTools: ";
    private static final String SPLIT_PATTERN = "\\||,|/";
    private static final String subtitleDelimiter = PropertiesUtil.getProperty("mjb.subtitle.delimiter", Movie.SPACE_SLASH_SPACE);
    private static final boolean subtitleUnique = PropertiesUtil.getBooleanProperty("mjb.subtitle.unique", Boolean.TRUE);
    private static final List<String> skippedSubtitles = new ArrayList<String>();
    private static final String YES = "YES";
    private static final String NO = "NO";

    static {
        // process allowed subtitles
        List<String> types = Arrays.asList(PropertiesUtil.getProperty("mjb.subtitle.skip", "").split(","));
        for (String type : types) {
            String determined = MovieFilenameScanner.determineLanguage(type.trim());
            if (StringTools.isValidString(determined)) {
                skippedSubtitles.add(determined.toUpperCase());
            }
        }
    }

    private SubtitleTools() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    /**
     * Set subtitles in the movie. Note: overrides the actual subtitles in movie.
     *
     * @param movie
     * @param parsedSubtitles
     */
    public static void setMovieSubtitles(Movie movie, Collection<String> subtitles) {
        if (!subtitles.isEmpty()) {

            // holds the subtitles for the movie
            String movieSubtitles = "";

            for (String subtitle : subtitles) {
                movieSubtitles = addMovieSubtitle(movieSubtitles, subtitle);
            }

            // set valid subtitles in movie; overwrites existing subtitles
            if (StringTools.isValidString(movieSubtitles)) {
                movie.setSubtitles(movieSubtitles);
            }
        }
    }

    /**
     * Adds a subtitle to the subtitles in the movie.
     *
     * @param movie
     * @param subtitle
     */
    public static void addMovieSubtitle(Movie movie, String subtitle) {
        String newSubtitles = addMovieSubtitle(movie.getSubtitles(), subtitle);
        movie.setSubtitles(newSubtitles);
    }

    /**
     * Adds a new subtitle to the actual list of subtitles.
     *
     * @param actualSubtitles
     * @param newSubtitle
     * @return new subtitles string
     */
    public static String addMovieSubtitle(String actualSubtitles, String newSubtitle) {
        // Determine the language
        String infoLanguage = MovieFilenameScanner.determineLanguage(newSubtitle);

        // Default value
        String newMovieSubtitles = actualSubtitles;

        if (StringTools.isValidString(infoLanguage) && !isSkippedSubtitle(infoLanguage)) {
            if (StringTools.isNotValidString(actualSubtitles) || actualSubtitles.equalsIgnoreCase(NO)) {
                // Overwrite existing sub titles
                newMovieSubtitles = infoLanguage;
            } else if (YES.equalsIgnoreCase(newSubtitle)) {
                LOG.trace(LOG_MESSAGE + "Subtitles already exist");
                // Nothing to change, cause there are already valid subtitle languages present
                // TODO Inspect if UNKNOWN should be added add the end of the subtitles list
            } else if (YES.equalsIgnoreCase(actualSubtitles)) {
                // override with subtitle language
                newMovieSubtitles = infoLanguage;
                // TODO Inspect if UNKNOWN should be added add the end of the subtitles list
            } else if (!subtitleUnique || !actualSubtitles.contains(infoLanguage)) {
                // Add subtitle to subtitles list
                newMovieSubtitles = actualSubtitles + subtitleDelimiter + infoLanguage;
            }
        }

        return newMovieSubtitles;
    }

    private static boolean isSkippedSubtitle(String language) {
        if (skippedSubtitles.isEmpty()) {
            // not skipped if list is empty
            return false;
        }

        boolean skipped = skippedSubtitles.contains(language.toUpperCase());
        if (skipped) {
            LOG.debug(LOG_MESSAGE + "Skipping subtitle '" + language + "'");
        }
        return skipped;
    }

    public static List<String> getSubtitles(Movie movie) {
        if (StringTools.isNotValidString(movie.getSubtitles())
                || YES.equalsIgnoreCase(movie.getSubtitles())
                || NO.equalsIgnoreCase(movie.getSubtitles())) {
            // skip none-language subtitles
            return Collections.emptyList();
        }

        // write out known subtitle languages
        return StringTools.splitList(movie.getSubtitles(), SPLIT_PATTERN);
    }
}
