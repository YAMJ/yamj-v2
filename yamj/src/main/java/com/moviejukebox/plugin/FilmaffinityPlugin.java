/*
/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import org.apache.commons.lang.StringUtils;

public class FilmaffinityPlugin extends ImdbPlugin {

    /*
     * Literals of web of each movie info
     */
//    private final String FA_ORIGINAL_TITLE = "<b>T\u00CDTULO ORIGINAL</b>";
//    private final String FA_YEAR = "<b>A\u00D1O</b>";
//    private final String FA_RUNTIME = "<b>DURACI\u00D3N</b>";
//    private final String FA_DIRECTOR = "<b>DIRECTOR</b>";
//    private final String FA_WRITER = "<b>GUI\u00D3N</b>";
//    private final String FA_CAST = "<b>REPARTO</b>";
//    private final String FA_GENRE = "<b>G\u00C9NERO</b>";
//    private final String FA_COMPANY = "<b>PRODUCTORA</b>";
//    private final String FA_PLOT = "<b>SINOPSIS</b>";
    private final String FA_ORIGINAL_TITLE = "<th>T&Iacute;TULO ORIGINAL</th>";
    private final String FA_YEAR = "<th>A&Ntilde;O</th>";
    private final String FA_RUNTIME = "<th>DURACI&Oacute;N</th>";
    private final String FA_DIRECTOR = "<th>DIRECTOR</th>";
    private final String FA_WRITER = "<th>GUI&Oacute;N</th>";
    private final String FA_CAST = "<th>REPARTO</th>";
    private final String FA_GENRE = "<th>G&Eacute;NERO</th>";
    private final String FA_COMPANY = "<th>PRODUCTORA</th>";
    private final String FA_PLOT = "<th>SINOPSIS</th>";
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
        String xml;
        String spanishTitle;
        Pattern countryPattern = Pattern.compile("<img src=\"/imgs/countries/[A-Z]{2}\\.jpg\" title=\"([\\w ]+)\"");
        Matcher countryMatcher;

        String filmAffinityId = movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID);

        if (StringTools.isNotValidString(filmAffinityId)) {
            logger.debug("FilmAffinity: No valid FilmAffinity ID for movie " + movie.getBaseName());
            return false;
        }

        try {
            xml = webBrowser.request("http://www.filmaffinity.com/es/" + filmAffinityId, Charset.forName("ISO-8859-1"));

            if (xml.contains("Serie de TV")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    logger.debug("FilmAffinity: " + movie.getTitle() + " is a TV Show, skipping.");
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            spanishTitle = HTMLTools.getTextAfterElem(xml, "<img src=\"http://www.filmaffinity.com/images/movie.gif\" border=\"0\">").replaceAll("\\s{2,}", " ");
            if (!movie.isOverrideTitle()) {
                movie.setTitle(spanishTitle);
                movie.setOriginalTitle(HTMLTools.getTextAfterElem(xml, FA_ORIGINAL_TITLE));
            }

            movie.setYear(HTMLTools.getTextAfterElem(xml, FA_YEAR));
            // check to see if the year is numeric, if not, try a different approach
            if (!StringUtils.isNumeric(movie.getYear())) {
                movie.setYear(HTMLTools.getTextAfterElem(xml, FA_YEAR, 1));
            }

            String runTime = HTMLTools.getTextAfterElem(xml, FA_RUNTIME, 1).replace(" min.", "m");
            if (!runTime.equals("min.")) {
                movie.setRuntime(runTime);
            }

            countryMatcher = countryPattern.matcher(xml);
            if (countryMatcher.find()) {
                movie.setCountry(countryMatcher.group(1));
            }

            for (String director : HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_DIRECTOR, "</a></td>")).split(",")) {
                movie.addDirector(director.trim());
            }

            /*
             * Sometimes FilmAffinity includes the writer of novel in the form:
             * screenwriter (Novela: writer)
             * OR
             * screenwriter1, screenwriter2, ... (Novela: writer)
             * OR even!!
             * screenwriter1, screenwriter2, ... (Story: writer1, writer2)
             *
             * The info between parenthesis doesn't add.
             */
            for (String writer : HTMLTools.getTextAfterElem(xml, FA_WRITER).split("\\(")[0].split(",")) {
                movie.addWriter(writer.trim());
            }

            for (String actor : HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_CAST, "</a></td>")).split(",")) {
                movie.addActor(actor.trim());
            }

            // TODO: Save more than one company.
            movie.setCompany(HTMLTools.getTextAfterElem(xml, FA_COMPANY).split("/")[0].trim());

            for (String genre : HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_GENRE, "</td>")).split("\\.|\\|")) {
                movie.addGenre(Library.getIndexingGenre(cleanStringEnding(genre.trim())));
            }

            try {
                movie.addRating(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, (int) (Float.parseFloat(HTMLTools.extractTag(xml, "<td align=\"center\" style=\"color:#990000; font-size:22px; font-weight: bold;\">", "</td>").replace(",", ".")) * 10));
            } catch (Exception e) {
                // Don't set a rating
            }

            String plot = HTMLTools.getTextAfterElem(xml, FA_PLOT);
            if (plot.endsWith("(FILMAFFINITY)")) {
                plot = new String(plot.substring(0, plot.length() - 14));
            }
            movie.setPlot(plot.trim());

            /*
             * Fill the rest of the fields from IMDB,
             * taking care not to allow the title to get overwritten.
             *
             * I change temporally: title = Original title
             * to improve the chance to find the right movie in IMDb.
             */
            boolean overrideTitle = movie.isOverrideTitle();
            String title = movie.getTitle();
            String originalTitle = movie.getOriginalTitle();
            movie.setOverrideTitle(true);
            movie.setTitle(movie.getOriginalTitle());
            super.scan(movie);
            // Change the title back to the way it was
            movie.setTitle(title);
            movie.setOriginalTitle(originalTitle);
            movie.setOverrideTitle(overrideTitle);

        } catch (Exception error) {
            logger.error("FilmAffinity: Failed retreiving movie info: " + filmAffinityId);
            logger.error(SystemTools.getStackTrace(error));
            returnStatus = false;
        }
        return returnStatus;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        super.scanTVShowTitles(movie);
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
