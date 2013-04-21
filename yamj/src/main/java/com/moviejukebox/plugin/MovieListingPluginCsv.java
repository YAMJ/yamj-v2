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
package com.moviejukebox.plugin;

import com.moviejukebox.model.*;
import com.moviejukebox.tools.CSVWriter;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 * User: JDGJr Date: Feb 15, 2009
 */
public class MovieListingPluginCsv extends MovieListingPluginBase implements MovieListingPlugin {

    private static final Logger logger = Logger.getLogger(MovieListingPluginCsv.class);

    /**
     * @return CSV-formatted header row
     */
    protected String headerLine() {
        StringBuilder headerLine = new StringBuilder();

        headerLine.append(prepOutput("Type"));
        headerLine.append(prepOutput("Title"));
        headerLine.append(prepOutput("TitleSort"));
        headerLine.append(prepOutput("IMDB ID"));
        headerLine.append(prepOutput("Director"));
        headerLine.append(prepOutput("Company"));
        headerLine.append(prepOutput("Country"));
        headerLine.append(prepOutput("Language"));
        headerLine.append(prepOutput("Runtime"));
        headerLine.append(prepOutput("Release Date"));
        headerLine.append(prepOutput("Year"));
        headerLine.append(prepOutput("Certification"));
        headerLine.append(prepOutput("Season #"));
        headerLine.append(prepOutput("TheTVDB ID"));
        headerLine.append(prepOutput("VideoSource"));
        headerLine.append(prepOutput("Container"));
        headerLine.append(prepOutput("File"));
        headerLine.append(prepOutput("Audio Codec"));
        headerLine.append(prepOutput("Audio Channels"));
        headerLine.append(prepOutput("Resolution"));
        headerLine.append(prepOutput("Video Codec"));
        headerLine.append(prepOutput("Video Output"));
        headerLine.append(prepOutput("FPS"));
        headerLine.append(prepOutput("# Files"));
        headerLine.append(prepOutput("# Extras"));
        headerLine.append(prepOutput("# Genres"));
        headerLine.append(prepOutput("# Cast"));
        headerLine.append(prepOutput("SubTitles?"));
        headerLine.append(prepOutput("Poster?"));
        headerLine.append(prepOutput("Detail Poster Filename"));
        headerLine.append(prepOutput("Rating #"));
        headerLine.append(prepOutput("Top 250 #"));
        headerLine.append(prepOutput("Library Description"));
        headerLine.append(prepOutput("Library Path"));
        headerLine.append(prepOutput("Allocine ID"));
        headerLine.append(prepOutput("FilmDelta ID"));
        headerLine.append(prepOutput("FilmUpIT ID"));
        headerLine.append(prepOutput("FilmWeb ID"));
        headerLine.append(prepOutput("Kinopoisk ID"));
        headerLine.append(prepOutput("Animator ID"));
        headerLine.append(prepOutput("Sratim ID"));
        headerLine.append(prepOutput("Last Modified Date"));
        headerLine.append(prepOutput("File Size"));
        headerLine.append(prepOutput("Watched", false));

        return headerLine.toString();
    } // headerLine();

    /**
     * @param sItemType
     * @param movie
     * @return output string properly formatted for CSV output
     */
    protected String toCSV(String sItemType, Movie movie) {
        Collection<ExtraFile> extras = movie.getExtraFiles();
        Collection<MovieFile> movieFiles = movie.getMovieFiles();
        Collection<String> genres = movie.getGenres();
        Collection<String> cast = movie.getCast();

        StringBuilder headerLine = new StringBuilder();

        headerLine.append(prepOutput(sItemType));
        headerLine.append(prepOutput(movie.getTitle()));
        headerLine.append(prepOutput(movie.getTitleSort()));
        headerLine.append(prepOutput(movie.getId(ImdbPlugin.IMDB_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getDirector()));
        headerLine.append(prepOutput(movie.getCompany()));
        headerLine.append(prepOutput(movie.getCountry()));
        headerLine.append(prepOutput(movie.getLanguage()));
        headerLine.append(prepOutput(movie.getRuntime()));
        headerLine.append(prepOutput(movie.getReleaseDate()));
        headerLine.append(prepOutput(movie.getYear()));
        headerLine.append(prepOutput(movie.getCertification()));
        headerLine.append(prepOutput(blankNegatives(movie.getSeason())));
        headerLine.append(prepOutput(movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getVideoSource()));
        headerLine.append(prepOutput(movie.getContainer()));
        headerLine.append(prepOutput(movie.getContainerFile().getAbsolutePath()));
        headerLine.append(prepOutput(movie.getAudioCodec()));
        headerLine.append(prepOutput(movie.getAudioChannels()));
        headerLine.append(prepOutput(movie.getResolution()));
        headerLine.append(prepOutput(movie.getVideoCodec()));
        headerLine.append(prepOutput(movie.getVideoOutput()));
        headerLine.append(prepOutput(Float.toString(movie.getFps())));
        headerLine.append(prepOutput(String.valueOf(null == movieFiles ? "0" : movieFiles.size())));
        headerLine.append(prepOutput(String.valueOf(null == extras ? "0" : extras.size())));
        headerLine.append(prepOutput(String.valueOf(null == genres ? "0" : genres.size())));
        headerLine.append(prepOutput(String.valueOf(null == cast ? "0" : cast.size())));
        headerLine.append(prepOutput(movie.getSubtitles()));
        headerLine.append(prepOutput((null != movie.getPosterURL() ? "True" : "False")));
        headerLine.append(prepOutput(String.valueOf(null != movie.getDetailPosterFilename() ? "True" : "False")));
        headerLine.append(prepOutput(String.valueOf(movie.getRating())));
        headerLine.append(prepOutput(String.valueOf(movie.getTop250())));
        headerLine.append(prepOutput(movie.getLibraryDescription()));
        headerLine.append(prepOutput(movie.getLibraryPath()));
        headerLine.append(prepOutput(movie.getId(AllocinePlugin.ALLOCINE_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getId(FilmUpITPlugin.FILMUPIT_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getId(KinopoiskPlugin.KINOPOISK_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getId(AnimatorPlugin.ANIMATOR_PLUGIN_ID)));
        headerLine.append(prepOutput(movie.getId(SratimPlugin.SRATIM_PLUGIN_ID)));
        headerLine.append(prepOutput(new Timestamp(movie.getLastModifiedTimestamp()).toString()));
        headerLine.append(prepOutput(movie.getFileSizeString()));
        headerLine.append(prepOutput(String.valueOf(movie.isWatched()), false));

        return headerLine.toString();
    } // toCSV()

    /**
     * @param i
     * @return empty string if input = -1
     */
    protected String blankNegatives(int i) {
        if (0 <= i) {
            return String.valueOf(i);
        } else {
            return "";
        }
    } // blankNegatives()

    /**
     * @param str
     * @return String cleaned up, ALWAYS comma appended
     */
    protected String prepOutput(String str) {
        return prepOutput(str, true);
    } // prepOutput()

    /**
     * @param newString
     * @param bAddComma
     * @return String cleaned up, optional comma appended
     */
    protected String prepOutput(final String inputString, boolean bAddComma) {
        String newString;
        if (null == inputString || (isBlankUnknown() && (UNKNOWN.equals(inputString) || getUndefined().equals(inputString)))) {
            // clean 'UNKNOWN' values
            newString = "";
        } else {
            newString = inputString;

            // remove quotes from the string (before encapsulation)
            if (newString.contains("\"")) {
                newString = newString.replace("\"", "");
            }

            // enclose strings with commas in quotes
            if (newString.contains(",")) {
                newString = "\"" + newString + "\"";
            }
        }

        // add trailing comma unless otherwise requested
        if (bAddComma) {
            newString += ",";
        }

        return newString;
    } // encloseInQuotes()

    /**
     * @param tempJukeboxRoot
     * @param jukeboxRoot
     * @param library
     */
    @Override
    public void generate(Jukebox jukebox, Library library) {
        initialize(jukebox);

        String filename = getBaseFilename() + ".csv";
        File csvFile = new File(jukebox.getJukeboxTempLocation(), filename);

        ArrayList<String> alTypes = getSelectedTypes();
        try {
            CSVWriter writer = new CSVWriter(csvFile);
            logger.debug("  Writing CSV to: " + csvFile.getAbsolutePath());

            // write header line
            writer.line(headerLine());

            if (!isGroupByType()) {
                for (Movie movie : library.values()) {
                    logger.debug(movie.toString());

                    String sType;
                    if (movie.isExtra()) {
                        sType = typeExtra;
                    } else if (movie.isTVShow()) {
                        sType = typeTVShow;
                    } else {
                        sType = typeMovie;
                    }

                    if (null != sType && alTypes.contains(sType)) {
                        writer.line(toCSV(sType, movie));
                    }
                } // for movie
            } else {
                for (String thisType : alTypes) {
                    for (Movie movie : library.values()) {
                        String sType;
                        if (movie.isExtra()) {
                            sType = typeExtra;
                        } else if (movie.isTVShow()) {
                            sType = typeTVShow;
                        } else {
                            sType = typeMovie;
                        }

                        if (null != sType && thisType.equalsIgnoreCase(sType)) {
                            writer.line(toCSV(sType, movie));
                        }
                    }
                }
            }

            writer.close();
        } catch (IOException error) {
            logger.error(SystemTools.getStackTrace(error));
        }

        // move to configured (default) location
        copyListingFile(csvFile, filename);

    } // generate()
} // class MovieListingPluginCsv

