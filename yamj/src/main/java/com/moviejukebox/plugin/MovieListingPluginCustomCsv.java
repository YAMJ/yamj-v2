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
package com.moviejukebox.plugin;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.CSVWriter;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MovieListingPluginCustomCsv extends MovieListingPluginBase implements MovieListingPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MovieListingPluginCustomCsv.class);
    private List<String> mFields;
    private String mDelimiter = ",";
    private String mSecondDelimiter = "|";
    private SimpleDateFormat mDateFormatter = null;
    private Double mRatingFactor = null;
    private NumberFormat mRatingFormatter = null;
    private int limitCast;
    private int limitGenres;
    // The default list of fields
    private static final String DEFAULT_FIELDS = createDefaultFields();

    /**
     * Take a comma-separated list of field names and split them into separate fields
     *
     * @param aFields Text to split
     * @return Number of fields found
     */
    private int initFields(String aFields) {
        // Clear the current list (if there is one)
        mFields = new ArrayList<String>();

        for (StringTokenizer t = new StringTokenizer(aFields, ","); t.hasMoreTokens();) {
            String st = StringUtils.trimToNull(t.nextToken());
            if (st != null) {
                mFields.add(st);
            }
        }
        return mFields.size();
    }

    /**
     * @return CSV-formatted header row
     */
    private String headerLine() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> iterator = mFields.iterator(); iterator.hasNext();) {
            if (sb.length() > 0) {
                sb.append(mDelimiter);
            }
            sb.append(iterator.next());
        }
        return sb.toString();
    } // headerLine();

    /**
     * Check if a header filed matches a known type
     *
     * @param field The text we are checking
     * @param knownType The type to match
     * @return true if we got a match
     */
    private boolean checkHeaderField(String field, String knownType) {
        // See if we want all the header fields or not
        if (field == null || knownType == null) {
            return false; // no need to compare
        }

        if (field.equalsIgnoreCase(knownType)) {
            return true; // it matched
        }

        // Last try - Remove all spaces, and any trailing ? or #
        String type = knownType.replace((CharSequence) " ", (CharSequence) "");
        if (type.endsWith("?") || type.endsWith("#")) {
            type = type.substring(0, type.length() - 1).trim();
        }

        return field.equalsIgnoreCase(type);
    }

    /**
     * @param sItemType
     * @param movie
     * @return output string properly formatted for CSV output
     */
    @SuppressWarnings("deprecation")
    private String toCSV(String sItemType, Movie movie) {
        Collection<ExtraFile> extras = movie.getExtraFiles();
        Collection<MovieFile> movieFiles = movie.getMovieFiles();
        Collection<String> genres = movie.getGenres();
        Collection<String> cast = movie.getCast();

        StringBuilder sb = new StringBuilder();

        for (String header : mFields) {
            if (sb.length() > 0) {
                sb.append(mDelimiter);
            }
            if (checkHeaderField(header, "Type")) {
                sb.append(prep(sItemType));
            } else if (checkHeaderField(header, "Title")) {
                sb.append(prep(movie.getTitle()));
            } else if (checkHeaderField(header, "TitleSort")) {
                sb.append(prep(movie.getTitleSort()));
            }else if (checkHeaderField(header, "OriginalTitle")){
                sb.append(prep(movie.getOriginalTitle()));
            } else if (checkHeaderField(header, "IMDB ID")) {
                sb.append(prep(movie.getId(ImdbPlugin.IMDB_PLUGIN_ID)));
            } else if (checkHeaderField(header, "TheTVDB ID")) {
                sb.append(prep(movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID)));
            } else if (checkHeaderField(header, "Director")) {
                sb.append(prep(movie.getDirector()));
            } else if (checkHeaderField(header, "Company")) {
                sb.append(prep(movie.getCompany()));
            } else if (checkHeaderField(header, "Country")) {
                sb.append(prep(movie.getCountriesAsString()));
            } else if (checkHeaderField(header, "Language")) {
                sb.append(prep(movie.getLanguage()));
            } else if (checkHeaderField(header, "Runtime")) {
                sb.append(prep(movie.getRuntime()));
            } else if (checkHeaderField(header, "Release Date")) {
                sb.append(prep(movie.getReleaseDate()));
            } else if (checkHeaderField(header, "Year")) {
                sb.append(prep(movie.getYear()));
            } else if (checkHeaderField(header, "Certification")) {
                sb.append(prep(movie.getCertification()));
            } else if (checkHeaderField(header, "Season #")) {
                sb.append(prep(blankNegatives(movie.getSeason())));
            } else if (checkHeaderField(header, "VideoSource")) {
                sb.append(prep(movie.getVideoSource()));
            } else if (checkHeaderField(header, "Container")) {
                sb.append(prep(movie.getContainer()));
            } else if (checkHeaderField(header, "File")) {
                sb.append(prep(movie.getContainerFile().getAbsolutePath()));
            } else if (checkHeaderField(header, "Audio Codec")) {
                sb.append(prep(movie.getAudioCodec()));
            } else if (checkHeaderField(header, "Audio Channels")) {
                sb.append(prep(movie.getAudioChannels()));
            } else if (checkHeaderField(header, "Resolution")) {
                sb.append(prep(movie.getResolution()));
            } else if (checkHeaderField(header, "Video Codec")) {
                sb.append(prep(movie.getVideoCodec()));
            } else if (checkHeaderField(header, "Video Output")) {
                sb.append(prep(movie.getVideoOutput()));
            } else if (checkHeaderField(header, "FPS")) {
                sb.append(prep(Float.toString(movie.getFps())));
            } else if (checkHeaderField(header, "# Files")) {
                sb.append(prep(String.valueOf(null == movieFiles ? "0" : movieFiles.size())));
            } else if (checkHeaderField(header, "# Extras")) {
                sb.append(prep(String.valueOf(null == extras ? "0" : extras.size())));
            } else if (checkHeaderField(header, "# Genres")) {
                sb.append(prep(String.valueOf(null == genres ? "0" : genres.size())));
            } else if (checkHeaderField(header, "# Cast")) {
                sb.append(prep(String.valueOf(null == cast ? "0" : cast.size())));
            } else if (checkHeaderField(header, "SubTitles?")) {
                sb.append(prep(movie.getSubtitles()));
            } else if (checkHeaderField(header, "Poster?")) {
                sb.append(prep(String.valueOf(StringTools.isValidString(movie.getPosterFilename()) ? "True" : "False")));
            } else if (checkHeaderField(header, "Poster Filename")) {
                sb.append(prep(movie.getPosterFilename()));
            } else if (checkHeaderField(header, "Fanart?")) {
                sb.append(prep(String.valueOf(StringTools.isValidString(movie.getFanartFilename()) ? "True" : "False")));
            } else if (checkHeaderField(header, "Fanart Filename")) {
                sb.append(prep(movie.getFanartFilename()));
            } else if (checkHeaderField(header, "Rating #")) {
                if (mRatingFactor != null) {
                    double fr = mRatingFactor.doubleValue() * movie.getRating();
                    sb.append(prep(mRatingFormatter.format(fr)));
                } else {
                    sb.append(prep(Integer.toString(movie.getRating())));
                }
            } else if (checkHeaderField(header, "Top 250 #")) {
                sb.append(prep(Integer.toString(movie.getTop250())));
            } else if (checkHeaderField(header, "Library Description")) {
                sb.append(prep(movie.getLibraryDescription()));
            } else if (checkHeaderField(header, "Library Path")) {
                sb.append(prep(movie.getLibraryPath()));
            } else if (checkHeaderField(header, "Allocine ID")) {
                sb.append(prep(movie.getId(AllocinePlugin.ALLOCINE_PLUGIN_ID)));
            } else if (checkHeaderField(header, "FilmUpIT ID")) {
                sb.append(prep(movie.getId(FilmUpITPlugin.FILMUPIT_PLUGIN_ID)));
            } else if (checkHeaderField(header, "FilmWeb ID")) {
                sb.append(prep(movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID)));
            } else if (checkHeaderField(header, "Kinopoisk ID")) {
                sb.append(prep(movie.getId(KinopoiskPlugin.KINOPOISK_PLUGIN_ID)));
            } else if (checkHeaderField(header, "Animator ID")) {
                sb.append(prep(movie.getId(AnimatorPlugin.ANIMATOR_PLUGIN_ID)));
            } else if (checkHeaderField(header, "Sratim ID")) {
                sb.append(prep(movie.getId(SratimPlugin.SRATIM_PLUGIN_ID)));
            } else if (checkHeaderField(header, "Last Modified Date")) {
                if (mDateFormatter != null) {
                    sb.append(prep(mDateFormatter.format(new Date(movie.getLastModifiedTimestamp()))));
                } else {
                    sb.append(prep(new Timestamp(movie.getLastModifiedTimestamp()).toString()));
                }
            } else if (checkHeaderField(header, "File Size")) {
                sb.append(prep(movie.getFileSizeString()));
            } else if (checkHeaderField(header, "Genres")) {
                if (null != genres) {
                    int counter = 1;
                    StringBuilder tmp = new StringBuilder();
                    for (String string : genres) {
                        if (counter++ > limitGenres) {
                            break;
                        }

                        if (tmp.length() > 0) {
                            tmp.append(mSecondDelimiter);
                        }
                        tmp.append(string);
                    }
                    sb.append(prep(tmp.toString()));
                }
            } else if (checkHeaderField(header, "Cast")) {
                if (null != cast) {
                    int counter = 1;
                    StringBuilder tmp = new StringBuilder();
                    for (String string : cast) {
                        if (counter++ > limitCast) {
                            break;
                        }

                        if (tmp.length() > 0) {
                            tmp.append(mSecondDelimiter);
                        }
                        tmp.append(string);
                    }
                    sb.append(prep(tmp.toString()));
                }
            } else if (checkHeaderField(header, "Plot")) {
                sb.append(prep(movie.getPlot()));
            } else if (checkHeaderField(header, "Outline")) {
                sb.append(prep(movie.getOutline()));
            } else if (checkHeaderField(header, "Thumbnail Filename")) {
                sb.append(prep(movie.getThumbnailFilename()));
            } else if (checkHeaderField(header, "Detail Poster Filename")) {
                sb.append(prep(movie.getDetailPosterFilename()));
            } else if (checkHeaderField(header, "Watched")) {
                sb.append(prep(String.valueOf(movie.isWatched())));
            } else {
                LOG.debug("Unknown field: '" + header + "'");

            }
        }
        return sb.toString();
    } // toCSV()

    /**
     * @param i
     * @return empty string if input = -1
     */
    private String blankNegatives(int i) {
        if (0 <= i) {
            return String.valueOf(i);
        } else {
            return "";
        }
    } // blankNegatives()

    /**
     * @param str
     * @return String cleaned up, NEVER comma appended
     */
    private String prep(final String input) {
        String str = input;

        if (null == str || (isBlankUnknown() && (UNKNOWN.equals(str) || getUndefined().equals(str)))) {
            // clean 'UNKNOWN' values
            str = "";
        } else {
            // convert all whitespace to a single space
            str = str.replaceAll("[\\s]", " ").trim();

            // remove quotes from the string (before encapsulation)
            if (str.contains("\"")) {
                str = str.replace("\"", "");
            }

            // enclose strings with commas in quotes
            if (str.contains(",")) {
                str = "\"" + str + "\"";
            }
        }
        return str;
    } // prepOutput()

    /**
     * Generate the listing file
     *
     * @param jukebox
     * @param library
     */
    @Override
    public void generate(Jukebox jukebox, Library library) {
        initialize(jukebox);
        initFields(PropertiesUtil.getProperty("mjb.listing.csv.fields", DEFAULT_FIELDS));

        mDelimiter = PropertiesUtil.getProperty("mjb.listing.csv.delimiter", ",");
        mSecondDelimiter = PropertiesUtil.getProperty("mjb.listing.csv.secondDelimiter", "|");
        String dateFormat = PropertiesUtil.getProperty("mjb.listing.csv.dateFormat", "");
        String ratingFactor = PropertiesUtil.getProperty("mjb.listing.csv.ratingFactor", "1.00");
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);
        mRatingFormatter = new DecimalFormat("#0.00", decimalFormatSymbols);
        limitCast = PropertiesUtil.getIntProperty("mjb.listing.csv.limitCast", 100);
        limitGenres = PropertiesUtil.getIntProperty("mjb.listing.csv.limitGenres", 100);

        mRatingFactor = new Double(ratingFactor);
        if (dateFormat.length() > 1) {
            mDateFormatter = new SimpleDateFormat(dateFormat);
        }

        String filename = getBaseFilename() + ".csv";
        File csvFile = new File(jukebox.getJukeboxTempLocation(), filename);

        List<String> alTypes = getSelectedTypes();
        try {
            CSVWriter writer = new CSVWriter(csvFile);
            LOG.debug("  Writing CSV to: " + csvFile.getAbsolutePath());

            // write header line
            writer.line(headerLine());

            if (!isGroupByType()) {
                for (Movie movie : library.values()) {
                    LOG.debug(movie.toString());

                    String sType;
                    if (movie.isExtra()) {
                        sType = TYPE_EXTRA;
                    } else if (movie.isTVShow()) {
                        sType = TYPE_TV_SHOW;
                    } else {
                        sType = TYPE_MOVIE;
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
                            sType = TYPE_EXTRA;
                        } else if (movie.isTVShow()) {
                            sType = TYPE_TV_SHOW;
                        } else {
                            sType = TYPE_MOVIE;
                        }

                        if (null != sType && thisType.equalsIgnoreCase(sType)) {
                            writer.line(toCSV(sType, movie));
                        }
                    }
                }
            }
            writer.close();
        } catch (IOException error) {
            LOG.error(SystemTools.getStackTrace(error));
        }

        // move to configured (default) location
        copyListingFile(csvFile, filename);

    } // generate()

    /**
     * Create a set of default fields
     *
     * @return
     */
    private static String createDefaultFields() {
        StringBuilder df = new StringBuilder();
        df.append("Type").append(",");
        df.append("Title").append(",");
        df.append("TitleSort").append(",");
        df.append("OriginalTitle").append(",");
        df.append("IMDB ID").append(",");
        df.append("TheTVDB ID").append(",");
        df.append("Director").append(",");
        df.append("Company").append(",");
        df.append("Country").append(",");
        df.append("Language").append(",");
        df.append("Runtime").append(",");
        df.append("Release Date").append(",");
        df.append("Year").append(",");
        df.append("Certification").append(",");
        df.append("Season #").append(",");
        df.append("VideoSource").append(",");
        df.append("Container").append(",");
        df.append("File").append(",");
        df.append("Audio Codec").append(",");
        df.append("Audio Channels").append(",");
        df.append("Resolution").append(",");
        df.append("Video Codec").append(",");
        df.append("Video Output").append(",");
        df.append("FPS").append(",");
        df.append("# Files").append(",");
        df.append("# Extras").append(",");
        df.append("# Genres").append(",");
        df.append("# Cast").append(",");
        df.append("SubTitles").append(",");
        df.append("Poster").append(",");
        df.append("Poster Filename").append(",");
        df.append("Fanart").append(",");
        df.append("Fanart Filename").append(",");
        df.append("Rating #").append(",");
        df.append("Top 250 #").append(",");
        df.append("Library Description").append(",");
        df.append("Library Path").append(",");
        df.append("Allocine ID").append(",");
        df.append("FilmUpIT ID").append(",");
        df.append("FilmWeb ID").append(",");
        df.append("Kinopoisk ID").append(",");
        df.append("Animator ID").append(",");
        df.append("Sratim ID").append(",");
        df.append("Last Modified Date").append(",");
        df.append("File Size").append(",");
        df.append("Genres").append(",");
        df.append("Cast").append(",");
        df.append("Plot").append(",");
        df.append("Outline").append(",");
        df.append("Thumbnail Filename").append(",");
        df.append("Detail Poster Filename").append(",");
        df.append("Watched");
        return df.toString();
    }
} // class MovieListingPluginCustomCsv

