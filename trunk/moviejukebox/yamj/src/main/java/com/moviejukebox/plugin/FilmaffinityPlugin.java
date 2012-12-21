/*
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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.OverrideFlag;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class FilmaffinityPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(FilmaffinityPlugin.class);
    /*
     * Literals of web of each movie info
     */
    private static final String FA_ORIGINAL_TITLE = "<th>T&Iacute;TULO ORIGINAL</th>";
    private static final String FA_YEAR = "<th>A&Ntilde;O</th>";
    private static final String FA_RUNTIME = "<th>DURACI&Oacute;N</th>";
    private static final String FA_DIRECTOR = "<th>DIRECTOR</th>";
    private static final String FA_WRITER = "<th>GUI&Oacute;N</th>";
    private static final String FA_CAST = "<th>REPARTO</th>";
    private static final String FA_GENRE = "<th>G&Eacute;NERO</th>";
    private static final String FA_COMPANY = "<th>PRODUCTORA</th>";
    private static final String FA_PLOT = "<th>SINOPSIS</th>";
    private FilmAffinityInfo filmAffinityInfo;

    public FilmaffinityPlugin() {
        super();  // use IMDB if FilmAffinity doesn't know movie
        filmAffinityInfo = new FilmAffinityInfo();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getPluginID() {
        return FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        String filmAffinityId = filmAffinityInfo.arrangeId(movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID));

        if (StringTools.isNotValidString(filmAffinityId)) {
            filmAffinityId = filmAffinityInfo.getIdFromMovieInfo(movie.getTitle(), movie.getYear(), movie.getSeason());
        }

        if (StringTools.isValidString(filmAffinityId)) {
            movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityId);
        }

        return updateFilmAffinityMediaInfo(movie);
    }

    /**
     * Scan FilmAffinity html page for the specified movie
     */
    private boolean updateFilmAffinityMediaInfo(Movie movie) {
        Boolean returnStatus = true;
        Pattern countryPattern = Pattern.compile("<img src=\"/imgs/countries/[A-Z]{2}\\.jpg\" title=\"([\\w ]+)\"");

        String filmAffinityId = movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);

        if (StringTools.isNotValidString(filmAffinityId)) {
            logger.debug("FilmAffinity: No valid FilmAffinity ID for movie " + movie.getBaseName());
            return false;
        }

        try {
            String xml = webBrowser.request("http://www.filmaffinity.com/es/" + filmAffinityId, Charset.forName("ISO-8859-1"));

            if (xml.contains("Serie de TV")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    logger.debug("FilmAffinity: " + movie.getTitle() + " is a TV Show, skipping.");
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            if (OverrideTools.checkOverwriteTitle(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String spanishTitle = HTMLTools.getTextAfterElem(xml, "<img src=\"http://www.filmaffinity.com/images/movie.gif\" border=\"0\">").replaceAll("\\s{2,}", " ");
                movie.setTitle(spanishTitle, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }
            if (OverrideTools.checkOverwriteOriginalTitle(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                movie.setOriginalTitle(HTMLTools.getTextAfterElem(xml, FA_ORIGINAL_TITLE), FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteYear(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String year = HTMLTools.getTextAfterElem(xml, FA_YEAR);
                // check to see if the year is numeric, if not, try a different approach
                if (!StringUtils.isNumeric(year)) {
                    year = HTMLTools.getTextAfterElem(xml, FA_YEAR, 1);
                }
                movie.setYear(year, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteRuntime(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String runTime = HTMLTools.getTextAfterElem(xml, FA_RUNTIME, 1).replace(" min.", "m");
                if (!runTime.equals("min.")) {
                    movie.setRuntime(runTime, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
                }
            }
            
            if (OverrideTools.checkOverwriteCountry(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                Matcher countryMatcher = countryPattern.matcher(xml);
                if (countryMatcher.find()) {
                    movie.setCountry(countryMatcher.group(1), FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
                }
            }
            
            if (OverrideTools.checkOverwriteDirectors(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newDirectors = Arrays.asList(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_DIRECTOR, "</a></td>")).split(","));
                movie.setDirectors(newDirectors, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            /*
             * Sometimes FilmAffinity includes the writer of novel in the form:
             * screenwriter (Novela: writer) OR screenwriter1, screenwriter2,
             * ... (Novela: writer) OR even!! screenwriter1, screenwriter2, ...
             * (Story: writer1, writer2)
             *
             * The info between parenthesis doesn't add.
             */
            if (OverrideTools.checkOverwriteWriters(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newWriters = Arrays.asList(HTMLTools.getTextAfterElem(xml, FA_WRITER).split("\\(")[0].split(","));
                movie.setWriters(newWriters, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }
            
            if (OverrideTools.checkOverwriteActors(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newActors = Arrays.asList(HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_CAST, "</a></td>")).split(","));
                movie.setCast(newActors, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteCompany(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                // TODO: Save more than one company.
                movie.setCompany(HTMLTools.getTextAfterElem(xml, FA_COMPANY).split("/")[0].trim(), FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }
            
            if (OverrideTools.checkOverwriteGenres(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                List<String> newGenres = new ArrayList<String>();
                for (String genre : HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_GENRE, "</td>")).split("\\.|\\|")) {
                    newGenres.add(Library.getIndexingGenre(cleanStringEnding(genre.trim())));
                }
                movie.setGenres(newGenres, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }
            
            try {
                movie.addRating(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, (int) (Float.parseFloat(HTMLTools.extractTag(xml, "<td align=\"center\" style=\"color:#990000; font-size:22px; font-weight: bold;\">", "</td>").replace(",", ".")) * 10));
            } catch (Exception e) {
                // Don't set a rating
            }

            if (OverrideTools.checkOverwritePlot(movie, FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID)) {
                String plot = HTMLTools.getTextAfterElem(xml, FA_PLOT);
                if (plot.endsWith("(FILMAFFINITY)")) {
                    plot = new String(plot.substring(0, plot.length() - 14));
                }
                movie.setPlot(plot.trim(), FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);
            }
            
            /*
             * Fill the rest of the fields from IMDB, taking care not to allow
             * the title to get overwritten.
             *
             * I change temporally: title = Original title to improve the chance
             * to find the right movie in IMDb.
             */
            String title = movie.getTitle();
            String titleSource = movie.getOverrideSource(OverrideFlag.TITLE);
            String originalTitle = movie.getOriginalTitle();
            String originalTitleSource = movie.getOverrideSource(OverrideFlag.ORIGINALTITLE);
            movie.setTitle(originalTitle, titleSource);
            super.scan(movie);
            // Change the title back to the way it was
            if (OverrideTools.checkOverwriteTitle(movie, titleSource)) {
                movie.setTitle(title, titleSource);
            }
            if (OverrideTools.checkOverwriteOriginalTitle(movie, originalTitleSource)) {
                movie.setOriginalTitle(originalTitle, originalTitleSource);
            }

        } catch (Exception error) {
            logger.error("FilmAffinity: Failed retreiving movie info: " + filmAffinityId);
            logger.error(SystemTools.getStackTrace(error));
            returnStatus = false;
        }
        return returnStatus;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        Pattern filtroFAiD = Pattern.compile("http://www.filmaffinity.com/es/film([0-9]{6})\\.html|filmaffinity=((?:film)?[0-9]{6}(?:\\.html)?)|<id moviedb=\"filmaffinity\">((?:film)?[0-9]{6}(?:\\.html)?)</id>", Pattern.CASE_INSENSITIVE);
        Matcher nfoMatcher = filtroFAiD.matcher(nfo);

        boolean result = false;
        if (nfoMatcher.find()) {
            if (nfoMatcher.group(1) != null) {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(1)));
            } else if (nfoMatcher.group(2) != null) {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(2)));
            } else {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(3)));
            }
            result = true;
        }

        // Look for IMDb id
        super.scanNFO(nfo, movie);
        return result;
    }
}
