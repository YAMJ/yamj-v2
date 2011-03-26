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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;

public class FilmaffinityPlugin extends ImdbPlugin {

    /*
     * Literals of web of each movie info 
     */    
    private final String FA_ORIGINAL_TITLE = "<b>T\u00CDTULO ORIGINAL</b>";
    private final String FA_YEAR = "<b>A\u00D1O</b>";
    private final String FA_RUNTIME = "<b>DURACI\u00D3N</b>";
    private final String FA_DIRECTOR = "<b>DIRECTOR</b>";
    private final String FA_WRITER = "<b>GUI\u00D3N</b>";
    private final String FA_CAST = "<b>REPARTO</b>";
    private final String FA_GENRE = "<b>G\u00C9NERO</b>";
    private final String FA_COMPANY = "<b>PRODUCTORA</b>";    
    private final String FA_PLOT = "<b>SINOPSIS</b>";    
    
    private FilmAffinityInfo filmAffinityInfo;

    public FilmaffinityPlugin() {
        super();  // use IMDB if Filmaffinity doesn't know movie
        filmAffinityInfo = new FilmAffinityInfo();
        webBrowser = new WebBrowser();
    }

    public boolean scan(Movie movie) {
        String filmAffinityId = filmAffinityInfo.arrangeId(movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID));
        
        if (StringTools.isNotValidString(filmAffinityId)) {
            filmAffinityId = filmAffinityInfo.getIdFromMovieInfo(movie.getTitle(), movie.getYear(), movie.getSeason());
        }
        
        movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID,filmAffinityId);
        
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
        
        try {
            xml = webBrowser.request("http://www.filmaffinity.com/es/"+movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID), Charset.forName("ISO-8859-1"));

            if (xml.contains("Serie de TV")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
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
            
            String runTime = HTMLTools.getTextAfterElem(xml, FA_RUNTIME).replace(" min.","m");
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
            
            for (String genre : HTMLTools.removeHtmlTags(HTMLTools.extractTag(xml, FA_GENRE, "</a></td>")).split("\\.|\\|")) {
                movie.addGenre(Library.getIndexingGenre(cleanSeeMore(genre.trim())));
            }
            
            try {
                movie.setRating((int)(Float.parseFloat(HTMLTools.extractTag(xml,"<td align=\"center\" style=\"color:#990000; font-size:22px; font-weight: bold;\">","</td>").replace(",", ".")) * 10));
            } catch (Exception e) {
                movie.setRating(0);
            }                        
            
            movie.setPlot(HTMLTools.getTextAfterElem(xml, FA_PLOT));
            
            /*
             * Fill the rest of the fields from IMDB, 
             * taking care not to allow the title to get overwritten.
             * 
             * I change temporally: title = Original title 
             * to improve the chance to find the right movie in IMDb. 
             */
            boolean overrideTitle = movie.isOverrideTitle();
            movie.setOverrideTitle(true);            
            movie.setTitle(movie.getOriginalTitle());
            super.scan(movie);
            movie.setTitle(spanishTitle);
            movie.setOverrideTitle(overrideTitle);
                        
        } catch (Exception error) {
            logger.error("Failed retreiving FA data movie : " + movie.getId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
            returnStatus = false;
        }
        return returnStatus;
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        super.scanTVShowTitles(movie);        
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        Pattern filtroFAiD = Pattern.compile("http://www.filmaffinity.com/es/film([0-9]{6})\\.html|filmaffinity=((?:film)?[0-9]{6}(?:\\.html)?)|<id moviedb=\"filmaffinity\">((?:film)?[0-9]{6}(?:\\.html)?)</id>",Pattern.CASE_INSENSITIVE);
        Matcher nfoMatcher = filtroFAiD.matcher(nfo);

        if (nfoMatcher.find()) {
            if (nfoMatcher.group(1) != null) {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(1)));                
            } 
            else if (nfoMatcher.group(2) != null) {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(2)));                                
            }
            else {
                movie.setId(FilmAffinityInfo.FILMAFFINITY_PLUGIN_ID, filmAffinityInfo.arrangeId(nfoMatcher.group(3)));                                                
            }
        }
        
        // Look for IMDb id
        super.scanNFO(nfo, movie);
    }
    
}
