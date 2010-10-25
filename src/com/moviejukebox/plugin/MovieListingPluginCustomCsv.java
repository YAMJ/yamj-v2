/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

package com.moviejukebox.plugin;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.logging.Logger;

import com.moviejukebox.model.ExtraFile;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.CSVWriter;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;

/**
 * User: nmn Date: Aug 15, 2010
 */
public class MovieListingPluginCustomCsv extends MovieListingPluginBase implements MovieListingPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private List<String> mFields;
    private String mDelimiter = ",";
    private String mSecondDelimiter = "|";
    private SimpleDateFormat mDateFormatter = null;
    private Double mRatingFactor = null;
    private NumberFormat mRatingFormatter = null;
    private int limitCast;
    private int limitGenres;

    // The default list of fields
    private static final String DEFAULT_FIELDS = "Type," + "Title," + "TitleSort," + "IMDB ID," + "TheTVDB ID," + "Director," 
        + "Company," + "Country," + "Language," + "Runtime," + "Release Date," + "Year," + "Certification," + "Season #," + "VideoSource," 
        + "Container," + "File," + "Audio Codec," + "Audio Channels," + "Resolution," + "Video Codec," + "Video Output," + "FPS," 
        + "# Files," + "# Extras," + "# Genres," + "# Cast," + "SubTitles?," + "Poster?," + "Poster Filename," + "Fanart?," 
        + "Fanart Filename," + "Rating #," + "Top 250 #," + "Library Description," + "Library Path," + "Allocine ID," + "FilmDelta ID," 
        + "FilmUpIT ID," + "FilmWeb ID," + "Kinopoisk ID," + "Sratim ID," + "Last Modified Date," + "File Size," + "Genres," + "Cast," 
        + "Plot," + "Outline," + "Thumbnail Filename," + "Detail Poster Filename"; 
    
    /**
     * Take a comma-separated list of field names and split them into separate fields
     * 
     * @param aFields
     *            Text to split
     * @return Number of fields found
     */
    private int initFields(String aFields) {
        ArrayList<String> list = new ArrayList<String>(50); // Ensure capacity
        for (StringTokenizer t = new StringTokenizer(aFields, ","); t.hasMoreTokens();) {
            String st = t.nextToken();
            if (st != null && st.trim().length() > 0) {
                list.add(st);
            }
        }
        list.trimToSize();
        mFields = list;
        return list.size();
    } // initFields()

    /**
     * @return CSV-formatted header row
     */
    private String headerLine() {
        StringBuffer sb = new StringBuffer();
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
     * @param knownType  The type to match
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
        String type = knownType.replace((CharSequence)" ", (CharSequence)"");
        if (type.endsWith("?") || type.endsWith("#")) {
            type = type.substring(0, type.length() - 1).trim();
        }
        
        if (field.equalsIgnoreCase(type)) {
            return true;
        }
        
        return false;
    } // checkHeaderField()

    /**
     * @param sItemType
     * @param movie
     * @return output string properly formatted for CSV output
     */
    private String toCSV(String sItemType, Movie movie) {
        Collection<ExtraFile> extras = movie.getExtraFiles();
        Collection<MovieFile> movieFiles = movie.getMovieFiles();
        Collection<String> genres = movie.getGenres();
        Collection<String> cast = movie.getCast();

        StringBuffer sb = new StringBuffer();

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
            } else if (checkHeaderField(header, "IMDB ID")) {
                sb.append(prep(movie.getId(ImdbPlugin.IMDB_PLUGIN_ID)));
            } else if (checkHeaderField(header, "TheTVDB ID")) {
                sb.append(prep(movie.getId(TheTvDBPlugin.THETVDB_PLUGIN_ID)));
            } else if (checkHeaderField(header, "Director")) {
                sb.append(prep(movie.getDirector()));
            } else if (checkHeaderField(header, "Company")) {
                sb.append(prep(movie.getCompany()));
            } else if (checkHeaderField(header, "Country")) {
                sb.append(prep(movie.getCountry()));
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
                sb.append(prep("" + (null == movieFiles ? "0" : movieFiles.size())));
            } else if (checkHeaderField(header, "# Extras")) {
                sb.append(prep("" + (null == extras ? "0" : extras.size())));
            } else if (checkHeaderField(header, "# Genres")) {
                sb.append(prep("" + (null == genres ? "0" : genres.size())));
            } else if (checkHeaderField(header, "# Cast")) {
                sb.append(prep("" + (null == cast ? "0" : cast.size())));
            } else if (checkHeaderField(header, "SubTitles?")) {
                sb.append(prep(movie.getSubtitles()));
            } else if (checkHeaderField(header, "Poster?")) {
                sb.append(prep("" + (StringTools.isValidString(movie.getPosterFilename()) ? "True" : "False")));
            } else if (checkHeaderField(header, "Poster Filename")) {
                sb.append(prep(movie.getPosterFilename()));
            } else if (checkHeaderField(header, "Fanart?")) {
                sb.append(prep("" + (StringTools.isValidString(movie.getFanartFilename()) ? "True" : "False")));
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
            } else if (checkHeaderField(header, "FilmDelta ID")) {
                sb.append(prep(movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID)));
            } else if (checkHeaderField(header, "FilmUpIT ID")) {
                sb.append(prep(movie.getId(FilmUpITPlugin.FILMUPIT_PLUGIN_ID)));
            } else if (checkHeaderField(header, "FilmWeb ID")) {
                sb.append(prep(movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID)));
            } else if (checkHeaderField(header, "Kinopoisk ID")) {
                sb.append(prep(movie.getId(KinopoiskPlugin.KINOPOISK_PLUGIN_ID)));
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
                    StringBuffer tmp = new StringBuffer();
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
                    StringBuffer tmp = new StringBuffer();
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
            }
        }
        return sb.toString();
    } // toCSV()

    /**
     * @param i
     * @return empty string if input = -1
     */
    private String blankNegatives(int i) {
        String sResult = "";
        if (0 <= i) {
            sResult = "" + i;
        }
        return sResult;
    } // blankNegatives()

    /**
     * @param str
     * @return String cleaned up, NEVER comma appended
     */
    private String prep(String input) {
        String str = input;
        
        if (null == str || (blankUNKNOWN && UNKNOWN.equals(str))) {
            // clean 'UNKNOWN' values
            str = "";
        }        
        // convert all whitespace to a single space
        str = str.replaceAll("[\\s]", " ").trim();;
        // remove quotes from the string (before encapsulation)
        if (str.contains("\"")) {
            str = str.replace("\"", "");
        }

        // enclose strings with commas in quotes
        if (str.contains(",")) {
            str = "\"" + str + "\"";
        }
        return str;
    } // prepOutput()

    /**
     * Generate the listing file
     * @param jukebox
     * @param library
     */
    public void generate(Jukebox jukebox, Library library) {
        initialize(jukebox);
        String fields = PropertiesUtil.getProperty("mjb.listing.csv.fields", DEFAULT_FIELDS);
        if (!StringTools.isValidString(fields)) {
            // If the "fields" is blank, populate it with the default
            fields = DEFAULT_FIELDS;
        }
        initFields(fields);
        
        mDelimiter = PropertiesUtil.getProperty("mjb.listing.csv.delimiter", ",");
        mSecondDelimiter = PropertiesUtil.getProperty("mjb.listing.csv.secondDelimiter", "|");
        String dateFormat = PropertiesUtil.getProperty("mjb.listing.csv.dateFormat", "");
        String ratingFactor = PropertiesUtil.getProperty("mjb.listing.csv.ratingFactor", "1.00");
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);
        mRatingFormatter = new DecimalFormat("#0.00", decimalFormatSymbols);
        
        try {
            limitCast = Integer.parseInt(PropertiesUtil.getProperty("mjb.listing.csv.limitCast", "100"));
        } catch (Exception ignore) {
            limitCast = 100;
        }

        try {
            limitGenres = Integer.parseInt(PropertiesUtil.getProperty("mjb.listing.csv.limitGenres", "100"));
        } catch (Exception ignore) {
            limitGenres = 100;
        }

        mRatingFactor = new Double(ratingFactor);
        if (dateFormat.length() > 1) {
            mDateFormatter = new SimpleDateFormat(dateFormat);
        }

        String filename = baseFilename + ".csv";
        File csvFile = new File(jukebox.getJukeboxTempLocation(), filename);

        ArrayList<String> alTypes = getSelectedTypes();
        try {
            CSVWriter writer = new CSVWriter(csvFile);
            logger.finer("  Writing CSV to: " + csvFile.getAbsolutePath());

            // write header line
            writer.line(headerLine());

            if (!groupByType) {
                for (Movie movie : library.values()) {
                    logger.finer(movie.toString());

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
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }

        // move to configured (default) location
        copyListingFile(csvFile, filename);

    } // generate()

} // class MovieListingPluginCustomCsv

