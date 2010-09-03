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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class AllocinePlugin extends ImdbPlugin {

    public static String ALLOCINE_PLUGIN_ID = "allocine";
    public static final Pattern DIACRITICS_AND_FRIENDS = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

    public AllocinePlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "France");
    }

    private int preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));

    /**
     * Scan Allocine html page for the specified TV Show
     */
    protected void updateTVShowInfo(Movie movie) {
        try {
            String xml = webBrowser.request("http://www.allocine.fr/series/ficheserie_gen_cserie=" + movie.getId(ALLOCINE_PLUGIN_ID) + ".html");

            if (!movie.isOverrideTitle()) {
                movie.setTitle(HTMLTools.extractTag(xml, "<h1>", "</h1>"));
            }
            movie.setRating(parseRating(HTMLTools.extractTag(xml, "<p class=\"withstars\">", "</p>")));
            // logger.finest("AllocinePlugin: TV Show rating = " + movie.getRating());
            String tmpPlot = removeHtmlTags(HTMLTools.extractTag(xml, "Synopsis :", "</p>"));
            // limit plot to ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT char
            if (tmpPlot.length() > preferredPlotLength) {
                tmpPlot = tmpPlot.substring(0, Math.min(tmpPlot.length(), preferredPlotLength - 3)) + "...";
            }
            movie.setPlot(tmpPlot);
            // logger.finest("AllocinePlugin: TV Show Plot = " + movie.getPlot());
            movie.setDirector(removeHtmlTags(HTMLTools.extractTag(xml, "Créée par", "</span>")));
            // logger.finest("AllocinePlugin: TV Show Director = " + movie.getDirector());

            // movie.setRuntime(HTMLTools.extractTag(xml, "Format</span> : ", "."));
            movie.setCountry(removeOpenedHtmlTags(HTMLTools.extractTag(xml, "<a href='/film/tous/", "</a>")));
            // logger.finest("AllocinePlugin: TV Show country = " + movie.getCountry());

            for (String genre : HTMLTools.extractTags(xml, "Genre :", "</p>", "<a href='/series/toutes/genre", "</a>", false)) {
                movie.addGenre(Library.getIndexingGenre(removeOpenedHtmlTags(genre)));
            }

            /*
             * Per user request Issue 1389
             * If a film does not have a certification - The phrase "Interdit aux moins de" is not found the "All" will be used
             * Otherwise the "??" value from "Interdit aux moins de ??" will be used 
             */
            int pos = xml.indexOf("Interdit aux moins de"); 
            if (pos == -1) {
                // Not found, so use "All"
                movie.setCertification("All");
            } else {
                // extract the age and use that as the certification
                movie.setCertification(HTMLTools.extractTag(xml, "Interdit aux moins de ", " ans"));
            }
            
            if (movie.isOverrideYear()) {
                movie.setYear(removeOpenedHtmlTags(HTMLTools.extractTag(xml, "en <a href=\"/series/toutes/", "</a>")));
                // logger.finest("AllocinePlugin: TV Show year = " + movie.getYear());
            }

            // Get Fanart
            if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
                movie.setFanartURL(getFanartURL(movie));
                if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                }
            }

            if (movie.getCast().isEmpty()) {
                for (String actor : HTMLTools.extractTags(xml, "Avec ", "<br />", "<a", "</a>", false)) {
                    // logger.finest("AllocinePlugin: actorTag=["+actor+"]");
                    if (actor.contains("/personne/fichepersonne_gen_cpersonne")) {
                        String cleanActor = removeOpenedHtmlTags(actor);
                        // logger.finest("AllocinePlugin: Actor added : " + cleanActor);
                        movie.addActor(cleanActor);
                    }
                }
            }

            // Removing Poster info from plugins. Use of PosterScanner routine instead.

            // Allocine does not old posters for Tv Shows
            // updatePoster(movie);
            // String posterURL = PosterScanner.getPosterURL(movie, xml, IMDB_PLUGIN_ID);
            // logger.finest("AllocinePlugin: Movie PosterURL from other source : " + posterURL);
            // movie.setPosterURL(posterURL);

            scanTVShowTitles(movie);
        } catch (Exception error) {
            logger.severe("AllocinePlugin: Failed retreiving alloCine TVShow info for " + movie.getId(ALLOCINE_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    public void scanTVShowTitles(Movie movie) {
        String allocineId = movie.getId(ALLOCINE_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || allocineId == null || allocineId.equalsIgnoreCase(Movie.UNKNOWN)) {
            return;
        }

        try {
            // start a new request for seasons details
            // logger.finest(
            // "AllocinePlugin: Start a new request for seasons details : http://www.allocine.fr/seriespage_seasonepisodes_last?cseries=" + allocineId);

            String xml = webBrowser.request("http://www.allocine.fr/seriespage_seasonepisodes_last?cseries=" + allocineId);

            for (String seasonTag : HTMLTools.extractHtmlTags(xml, "Saison :", "</ul>", "<a href=\"/seriespage_seasonepisodes_last", "</a>")) {
                try {
                    // logger.finest("AllocinePlugin: New Season detected seasonTag=[" + seasonTag+"]");
                    String seasonString = removeHtmlTags(seasonTag);
                    // logger.finest("AllocinePlugin: New Season detected seasonString=[" + seasonString+"]");
                    int seasonNbI = Integer.valueOf(seasonString);
                    // logger.finest("AllocinePlugin: New Season detected seasonNbI=[" + seasonNbI+"]");
                    String seasonStringId = seasonTag.substring(seasonTag.lastIndexOf('=') + 1, seasonTag.lastIndexOf('"'));
                    // logger.finest("AllocinePlugin: New Season detected seasonStringId=[" + seasonStringId+"]");
                    int seasonIdI = Integer.valueOf(seasonStringId);
                    // logger.finest("AllocinePlugin: New Season detected seasonIdI= " + seasonIdI);
                    // logger.finest("AllocinePlugin: seasonNbI = " + seasonNbI + ", movie.getSeason() = " + movie.getSeason());
                    if (seasonNbI == movie.getSeason()) {
                        // we found the right season, time to get the infos
                        // logger.finest("AllocinePlugin: The right SeasonIdI = " + seasonIdI);
                        xml = webBrowser.request("http://www.allocine.fr/seriespage_seasonepisodes_last?cseries=" + allocineId + "&cseriesseason=" + seasonIdI);

                        HashMap<Integer, String> episodeNames = new HashMap<Integer, String>();
                        for (String episode : HTMLTools.extractTags(xml, "<table class=\"boxofficedata\">", "</table>", "<a", "</a>", false)) {
                            try {
                                // logger.finest("AllocinePlugin: episodeTag=[" + episode+"]");
                                String episodeNbS = episode.substring(episode.lastIndexOf("Episode") + 8, episode.lastIndexOf(':')).trim();
                                // logger.finest("AllocinePlugin: episodeNbS=[" + episodeNbS+"]");
                                Integer episodeNbI = Integer.valueOf(episodeNbS);
                                String episodeName = episode.substring(episode.lastIndexOf(':') + 1).trim();
                                // logger.finest("AllocinePlugin: episodeName=[" + episodeName+"]");
                                episodeNames.put(episodeNbI, episodeName);
                                // logger.finest("AllocinePlugin: Episode Nb "+ episodeNbI + " : [" + episodeName+"] added to map");
                            } catch (Exception e) {
                                logger.severe("AllocinePlugin: Error while parsing episode names : " + e.getMessage());
                                // skip to next episode
                            }
                        }
                        for (MovieFile file : movie.getFiles()) {
                            if (!file.isNewFile() || file.hasTitle()) {
                                // don't scan episode title if it exists in XML data
                                continue;
                            }
                            logger.finest("AllocinePlugin: Setting filename for episode Nb " + file.getFirstPart());
                            file.setTitle(episodeNames.get(file.getFirstPart()));
                        }
                    }
                } catch (Exception error) {
                    // logger.severe("AllocinePlugin: Error while getting season infos " + e);
                    // nothing to do, we skip this season
                }
            }
        } catch (Exception error) {
            logger.severe("AllocinePlugin: Failed retreiving episodes titles for movie : " + movie.getTitle());
            logger.severe("AllocinePlugin: Error : " + error.getMessage());
        }
    }

    /**
     * Scan Allocine html page for the specified movie
     */
    private boolean updateMovieInfo(Movie movie) {
        try {
            String xml = webBrowser.request("http://www.allocine.fr/film/fichefilm_gen_cfilm=" + movie.getId(ALLOCINE_PLUGIN_ID) + ".html");

            if (xml.contains("Série créée")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            if (!movie.isOverrideTitle()) {
                movie.setTitle(HTMLTools.extractTag(xml, "<h1>", "</h1>"));
            }

            if (movie.getOriginalTitle().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setOriginalTitle(removeHtmlTags(HTMLTools.extractTag(xml, "Titre original :", "</em>")));
            }

            if (movie.getRating() == -1) {
                movie.setRating(parseRating(HTMLTools.extractTag(xml, "Note Moyenne:", "</span>")));
                // logger.finest("AllocinePlugin: Movie rating = " + movie.getRating());
            }

            if (movie.getPlot().equalsIgnoreCase(Movie.UNKNOWN)) {
                // limit plot to ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT char
                String tmpPlot = removeHtmlTags(HTMLTools.extractTag(xml, "Synopsis :", "</p>")).trim();
                // logger.finest("AllocinePlugin: tmpPlot = [" + tmpPlot + "]");
                if (tmpPlot.length() > preferredPlotLength) {
                    tmpPlot = tmpPlot.substring(0, Math.min(tmpPlot.length(), preferredPlotLength - 3)) + "...";
                }
                movie.setPlot(tmpPlot);
                // logger.finest("AllocinePlugin: Movie Plot = " + movie.getPlot());
            }

/*            if (movie.getDirector().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setDirector(removeHtmlTags(HTMLTools.extractTag(xml, "Réalisé par ", "</span>")));
                // logger.finest("AllocinePlugin: Movie Director = " + movie.getDirector());
            }*/

            if ( (movie.getReleaseDate().equals(Movie.UNKNOWN)) && (xml.indexOf("Date de sortie cinéma : <span>inconnue</span>") == -1) ) {
                Pattern yearRegex = Pattern.compile(".*(\\d{4})$");
                String releaseDate = removeHtmlTags(HTMLTools.extractTag(xml, "Date de sortie cinéma : ", "<br />"));
                Matcher match = yearRegex.matcher(releaseDate);
                if (match.find()) {
                    movie.setReleaseDate(releaseDate);
                }
                // logger.finest("AllocinePlugin: Movie Theater release date = [" + movie.getReleaseDate()+"]");
            }

            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                movie.setRuntime(HTMLTools.extractTag(xml, "Durée :", "Année"));
                // logger.finest("AllocinePlugin: Durée = " + movie.getRuntime());
            }

            if (movie.getCountry().equals(Movie.UNKNOWN)) {
                movie.setCountry(removeHtmlTags(HTMLTools.extractTag(xml, "Long-métrage", "</a>")).trim());
                // logger.finest("AllocinePlugin: Movie Country = " + movie.getCountry());
            }

            // if (movie.getCompany().equals(Movie.UNKNOWN)) {
            // movie.setCompany(removeHtmlTags(HTMLTools.extractTag(xml, "Distribué par ", "</h3>")));
            // logger.finest("AllocinePlugin: Movie Company = " + movie.getCompany());
            // }

            if (movie.getGenres().isEmpty()) {
                for (String genre : HTMLTools.extractTags(xml, "Genre", "<br />", "/film/tous/genre-", "</a>", false)) {
                    movie.addGenre(removeOpenedHtmlTags(genre));
                }
            }

            /*
             * Per user request Issue 1389
             * If a film does not have a certification - The phrase "Interdit aux moins de" is not found the "All" will be used
             * Otherwise the "??" value from "Interdit aux moins de ??" will be used 
             */
            int pos = xml.indexOf("Interdit aux moins de"); 
            if (pos == -1) {
                // Not found, so use "All"
                movie.setCertification("All");
            } else {
                // extract the age and use that as the certification
                movie.setCertification(HTMLTools.extractTag(xml, "Interdit aux moins de ", " ans"));
            }

            if (!movie.isOverrideYear() && movie.getYear().equals(Movie.UNKNOWN)) {

                Pattern yearRegex = Pattern.compile(".*(\\d{4})$");

                
                String aYear = movie.getReleaseDate();
                // Looking for the release date
                if (aYear.equals(Movie.UNKNOWN)) {
                    aYear = removeHtmlTags(HTMLTools.extractTag(xml, "Année de production : ", "</a>"));
                    Matcher match = yearRegex.matcher(aYear);
                    if (!match.find()) {
                        aYear = removeHtmlTags(HTMLTools.extractTag(HTMLTools.extractTag(xml, "Film déjà disponible en ", "<br />"), "</a> depuis le : "));
                        match = yearRegex.matcher(aYear);
                        if (!match.find()) {
                            aYear = Movie.UNKNOWN;
                        }
                    }
                } 
                if (!aYear.equals(Movie.UNKNOWN)) {
                    movie.setYear(aYear.substring(aYear.length() - 4));
                }
            }

            // Get Fanart
            if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
                movie.setFanartURL(getFanartURL(movie));
                if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                }
            }

            // Get Casting
            parseCasting(movie);
        } catch (IOException error) {
            logger.severe("AllocinePlugin: Failed retreiving allocine infos for movie : " + movie.getId(ALLOCINE_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return true;
    }

    private boolean parseCasting(Movie movie) {
        try {
            String xmlCasting = webBrowser.request("http://www.allocine.fr/film/casting_gen_cfilm=" + movie.getId(ALLOCINE_PLUGIN_ID) + ".html");
            
            // Check for director
            ArrayList<String> tempDirectors = HTMLTools.extractTags(xmlCasting, "<a class=\"anchor\" id='direction'></a>", "</div><!-- /rubric !-->", "<div class=\"datablock\">", "</div><!-- /datablock -->", false);
            if (movie.getDirector() == null || movie.getDirector().isEmpty() || movie.getDirector().equalsIgnoreCase(Movie.UNKNOWN)) {
                if (!tempDirectors.isEmpty()) {
                    movie.setDirector(removeHtmlTags(HTMLTools.extractTag(tempDirectors.get(0),"<h3>","</h3>")));
                }
            }

            // Check for writers
            // logger.finer("AllocinePlugin: Check for writers");
            if (movie.getWriters().isEmpty()) {
                for (String writer : HTMLTools.extractTags(xmlCasting, "<a class=\"anchor\" id=\"scenario\"></a>", "</table>", ".html\">", "</a>", false)) {
                    // logger.finer("AllocinePlugin: Writer found : " + writer);
                    movie.addWriter(removeHtmlTags(writer));
                }
            }
            
            // Check for company 
            if (movie.getCompany().equals(Movie.UNKNOWN)) {
                movie.setCompany(removeHtmlTags(HTMLTools.extractTag(xmlCasting, "Film distribué par ", "</a>")));
            }

            // Check for actors
            if (movie.getCast().isEmpty()) {
                for (String actor : HTMLTools.extractTags(xmlCasting, "<a class=\"anchor\" id='actors'></a>", "</div><!-- /rubric !-->", "<div class=\"datablock\">", "</div><!-- /datablock -->", false)) {
                    movie.addActor(removeHtmlTags(HTMLTools.extractTag(actor,"<h3>","</h3>")));
                }
            }
        } catch (IOException error) {
            logger.severe("AllocinePlugin: Failed retreiving allocine casting infos for movie : " + movie.getId(ALLOCINE_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return true;
    }

    private int parseRating(String rating) {

        try {
            int ratingBegin = rating.indexOf("(");
            int ratingEnd = rating.indexOf(")");
            String floatRating = rating.substring(ratingBegin + 1, ratingEnd).replace(',', '.');
            // logger.finest("AllocinePlugin: String floatRating =[" + floatRating + "]");

            return (int)(Float.parseFloat(floatRating) / 4.0 * 100);
        } catch (Exception error) {
            logger.finer("AllocinePlugin: AllocinePlugin.parseRating no rating found in [" + rating + "]");
            return -1;
        }
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = true;
        try {
            String allocineId = mediaFile.getId(ALLOCINE_PLUGIN_ID);
            if (allocineId.equalsIgnoreCase(Movie.UNKNOWN)) {
                allocineId = getAllocineId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.isTVShow() ? 0 : -1);
            }
            
            // we also get imdb Id for extra infos
            if (mediaFile.getId(IMDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(IMDB_PLUGIN_ID, imdbInfo.getImdbId(mediaFile.getOriginalTitle(), mediaFile.getYear()));
                logger.finest("AllocinePlugin: Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
            }
            
            if (!allocineId.equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(ALLOCINE_PLUGIN_ID, allocineId);
                if (mediaFile.isTVShow()) {
                    updateTVShowInfo(mediaFile);
                } else {
                    retval = updateMovieInfo(mediaFile);
                }
            } else {
                // If no AllocineId found fallback to Imdb
                logger.finer("AllocinePlugin: No Allocine Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (ParseException error) {
            // If no AllocineId found fallback to Imdb
            logger.finer("AllocinePlugin: Parse error. Now using ImdbPlugin");
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the allocineId matching the specified movie name. This routine is base on a alloCine search.
     * 
     * @throws ParseException
     */
    public String getAllocineId(String movieName, String year, int tvSeason) throws ParseException {
        if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
            return getAllocineIdFromGoogle(movieName, year);
        } else {
            String allocineId = Movie.UNKNOWN;
            // Add '/1' in the URL to obtain the 20 first matches
            String allocineBaseRequest = "http://www.allocine.fr/recherche/1/?q=";
            try {
                StringBuffer sb = new StringBuffer(allocineBaseRequest);
                sb.append(URLEncoder.encode(movieName, "UTF-8"));
                String xml = webBrowser.request(sb.toString());

                String alloCineStartResult;
                String alloCineMediaPrefix;
                if (tvSeason > -1) {
                    alloCineStartResult = "dans les titres de séries TV";
                    alloCineMediaPrefix = "/series/ficheserie_gen_cserie=";
                } else {
                    alloCineStartResult = "dans les titres de films";
                    alloCineMediaPrefix = "/film/fichefilm_gen_cfilm=";
                }

                String alloCineEndResult = "</table>";
                String alloCineMediaSectionTagEnd = "</span> <!--  /fs11 -->";

                String alloCineYearTagStart = "<span class=\"fs11\">";
                String alloCineYearTagEnd = "<br />";
            
                String formattedMovieName = Normalizer.normalize(movieName.replace("\u0153","oe").toLowerCase(), Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "");
                for (String searchResult : HTMLTools.extractTags(xml, alloCineStartResult, alloCineEndResult, alloCineMediaPrefix, alloCineMediaSectionTagEnd, false)) {
                    String formattedTitle = "<" + HTMLTools.extractTag(searchResult, alloCineMediaPrefix, "</a>").replace("<b>", "").replace("</b>", "");
                    formattedTitle = HTMLTools.getTextAfterElem(formattedTitle, ".html'>").replace(":", "-");
                    formattedTitle = Normalizer.normalize(formattedTitle, Normalizer.Form.NFD).replaceAll("[\u0300-\u036F]", "").replace(" (TV)", "");
                    // logger.finest("AllocinePlugin search '" + formattedMovieName + "' in " + searchResult);

                    if (formattedTitle.equalsIgnoreCase(formattedMovieName)) {
                        String searchResultYear = searchResult.substring(searchResult.lastIndexOf(alloCineYearTagStart) + alloCineYearTagStart.length(), searchResult
                                .length());
                        searchResultYear = searchResultYear.substring(0, searchResultYear.indexOf(alloCineYearTagEnd)).trim();
                        // Issue #1265: to prevent some strange layout where tags <b></b> are inside the movie year
                        searchResultYear = removeHtmlTags(searchResultYear);
                        logger.finest("AllocinePlugin: searchResultYear = [" + searchResultYear+"] while year=["+year+"]");
                        if (year == null || year.equalsIgnoreCase(Movie.UNKNOWN) || year.equalsIgnoreCase(searchResultYear)) {
                            int allocineIndexBegin = 0;
                            int allocineIndexEnd = searchResult.indexOf(".html");

                            allocineId = searchResult.substring(allocineIndexBegin, allocineIndexEnd);
                            // validate that allocineId is an int
                            Integer.valueOf(allocineId);
                            logger.finer("AllocinePlugin: Found AllocineId = " + allocineId);
                            return allocineId;
                        }
                    }
                }
                return Movie.UNKNOWN;
            } catch (Exception error) {
                logger.severe("AllocinePlugin: Failed to retrieve alloCine Id for movie : " + movieName);
                logger.severe("AllocinePlugin: Now using ImdbPlugin");
                throw new ParseException(allocineId, 0);
            }
        }
    }


    /**
     * Retrieve the AllocineId matching the specified movie name and year. This routine is base on a Google request.
     * 
     * @param movieName
     *            The name of the Movie to search for
     * @param year
     *            The year of the movie
     * @return The AllocineId if it was found
     */
    private String getAllocineIdFromGoogle(String movieName, String year) {
        try {
            StringBuffer sb = new StringBuffer("http://www.google.fr/search?hl=fr&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }
            sb.append("+site%3Awww.allocine.fr&meta=");
//            logger.finest("AllocinePlugin: Allocine request via Google : " + sb.toString());
            String xml = webBrowser.request(sb.toString());
            String allocineId = HTMLTools.extractTag(xml, "film/fichefilm_gen_cfilm=",".html");
//            logger.finest("AllocinePlugin: Allocine found via Google : " + allocineId);
            return allocineId;
        } catch (Exception error) {
            logger.severe("ImdbInfo Failed retreiving AlloCine Id for movie : " + movieName);
            logger.severe("ImdbInfo Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    protected String removeHtmlTags(String src) {
        String result = src.replaceAll("\\<.*?>", "").trim();
        // logger.finest("AllocinePlugin: removeHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
    }

    protected String removeOpenedHtmlTags(String src) {
        String result = src.replaceAll("^.*?>", "");
        result = result.replaceAll("<.*?$", "");
        result = result.trim();
        // logger.finest("AllocinePlugin: removeOpenedHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // If we use allocine plugin look for
        // http://www.allocine.fr/...=XXXXX.html
        logger.finest("AllocinePlugin: Scanning NFO for Allocine Id");
        int beginIndex = nfo.indexOf("http://www.allocine.fr/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf("=", beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf(".", beginIdIndex);
                if (endIdIndex != -1) {
                    logger.finer("AllocinePlugin: Allocine Id found in nfo = " + nfo.substring(beginIdIndex + 1, endIdIndex));
                    movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, nfo.substring(beginIdIndex + 1, endIdIndex));
                } else {
                    logger.finer("AllocinePlugin: No Allocine Id found in nfo !");
                }
            } else {
                logger.finer("AllocinePlugin: No Allocine Id found in nfo !");
            }
        } else {
            logger.finer("AllocinePlugin: No Allocine Id found in nfo !");
        }
    }
}
