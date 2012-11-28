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
package com.moviejukebox.tools;

import java.util.*;
import org.apache.log4j.Logger;
import com.moviejukebox.model.Movie;
import com.moviejukebox.scanner.MovieFilenameScanner;

public final class SubtitleTools {

    private static final Logger logger = Logger.getLogger(SubtitleTools.class);
    private static final String LOG_MESSAGE = "SubtitleTools: ";

    private static final String SPLIT_PATTERN = "\\||,|/";
    
    private static final String subtitleDelimiter = PropertiesUtil.getProperty("mjb.subtitle.delimiter", Movie.SPACE_SLASH_SPACE);
    private static final List<String> skippedSubtitles = new ArrayList<String>();
    
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
    
    /**
     * Set subtitles in the movie.
     * Note: overrides the actual subtitles in movie.
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
            if (StringTools.isNotValidString(actualSubtitles) || actualSubtitles.equalsIgnoreCase("NO") ) {
                // Overwrite existing sub titles
                newMovieSubtitles =  infoLanguage;
            } else if ("YES".equalsIgnoreCase(newSubtitle)) {
                // Nothing to change, cause there are already valid subtitle languages present
                // TODO Inspect if UNKNOWN should be added add the end of the subtitles list
            } else if ("YES".equalsIgnoreCase(actualSubtitles)) {
                // override with subtitle language
                newMovieSubtitles = infoLanguage;
                // TODO Inspect if UNKNOWN should be added add the end of the subtitles list
            } else if (!actualSubtitles.contains(infoLanguage)) {
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
            logger.debug(LOG_MESSAGE + "Skipping subtite '" + language + "'");
        }
        return skipped;
    }
    
    public static List<String> getSubtitles(Movie movie) {
        if (StringTools.isValidString(movie.getSubtitles())) {
            return StringTools.splitList(movie.getSubtitles(), SPLIT_PATTERN);
        }
        return Collections.emptyList();
    }
}
