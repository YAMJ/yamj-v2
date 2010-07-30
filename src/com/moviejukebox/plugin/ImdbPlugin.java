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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.ImdbSiteDataDefinition;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

public class ImdbPlugin implements MovieDatabasePlugin {

    public static String IMDB_PLUGIN_ID = "imdb";
    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected String preferredCountry;
    private String imdbPlot;
    protected WebBrowser webBrowser;
    protected boolean downloadFanart;
    private boolean extractCertificationFromMPAA;
    private boolean getFullInfo;
    protected String fanartToken;
    private int preferredPlotLength;
    private ImdbSiteDataDefinition siteDef;
    protected ImdbInfo imdbInfo;

    public ImdbPlugin() {
        imdbInfo = new ImdbInfo();
        siteDef = imdbInfo.getSiteDef();

        webBrowser = new WebBrowser();

        PropertiesUtil.getProperty("imdb.id.search", "imdb");
        Boolean.parseBoolean(PropertiesUtil.getProperty("imdb.perfect.match", "true"));
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
        imdbPlot = PropertiesUtil.getProperty("imdb.plot", "short");
        downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("fanart.movie.download", "false"));
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("plugin.plot.maxlength", "500"));
        extractCertificationFromMPAA = Boolean.parseBoolean(PropertiesUtil.getProperty("imdb.getCertificationFromMPAA", "true"));
        getFullInfo = Boolean.parseBoolean(PropertiesUtil.getProperty("imdb.full.info", "false"));
    }

    @Override
    public boolean scan(Movie mediaFile) {
        String imdbId = mediaFile.getId(IMDB_PLUGIN_ID);
        if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            imdbId = imdbInfo.getImdbId(mediaFile.getTitle(), mediaFile.getYear());
            mediaFile.setId(IMDB_PLUGIN_ID, imdbId);
        }

        boolean retval = true;
        if (!imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            retval = updateImdbMediaInfo(mediaFile);
        }
        return retval;
    }

    protected String getPreferredValue(ArrayList<String> values) {
        String value = Movie.UNKNOWN;
        for (String text : values) {
            String country = null;

            int pos = text.indexOf(':');
            if (pos != -1) {
                country = text.substring(0, pos);
                text = text.substring(pos + 1);
            }
            pos = text.indexOf('(');
            if (pos != -1) {
                text = text.substring(0, pos).trim();
            }

            if (country == null) {
                if (value.equals(Movie.UNKNOWN)) {
                    value = text;
                }
            } else {
                if (country.equals(preferredCountry)) {
                    value = text;
                }
            }
        }
        return HTMLTools.stripTags(value);
    }

    /**
     * Scan IMDB HTML page for the specified movie
     */
    private boolean updateImdbMediaInfo(Movie movie) {
        try {
            String xml = null;
            if (getFullInfo) {
                xml = webBrowser.request(siteDef.getSite() + "title/" + movie.getId(IMDB_PLUGIN_ID) + "/combined", siteDef.getCharset());
            } else {
                xml = webBrowser.request(siteDef.getSite() + "title/" + movie.getId(IMDB_PLUGIN_ID) + "/", siteDef.getCharset());
            }
            
            if (xml.contains("\"tv-extra\"")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            if (!movie.isOverrideTitle()) {
                String title = HTMLTools.extractTag(xml, "<title>");
                title = title.replaceAll(" \\([VG|V]\\)$", ""); // Remove the (VG) or (V) tags from the title
                title = title.replaceAll(" \\((\\d{4})(?:/[^\\)]+)?\\)", ""); // Remove the Date identifier
                movie.setTitle(title);
                movie.setOriginalTitle(title);
            }
            if (movie.getRating() == -1) {
                movie.setRating(parseRating(HTMLTools.extractTag(xml, "<div class=\"starbar-meta\">", 2).replace(",", ".")));
            }

            if (movie.getTop250() == -1) {
                try {
                    movie.setTop250(Integer.parseInt(HTMLTools.extractTag(xml, "Top 250: #")));
                } catch (NumberFormatException error) {
                    movie.setTop250(-1);
                }
            }

            if (movie.getDirector().equals(Movie.UNKNOWN)) {
                // Note this is a hack for the change to IMDB for Issue 875
                // TODO: Change the directors into a collection for better processing.
                ArrayList<String> tempDirectors = null;
                // Issue 1261 : Allow multiple text maching for one "element".
                String[] directorMatches = siteDef.getDirector().split("\\|");

                for (String directorMatch : directorMatches) {
                    tempDirectors = HTMLTools.extractTags(xml, "<h5>" + directorMatch, "</div>", "<a href=\"/name/", "</a>");
                    if (!tempDirectors.isEmpty()) {
                        // We match stop search.
                        break;
                    }
                }

                if (movie.getDirector() == null || movie.getDirector().isEmpty() || movie.getDirector().equalsIgnoreCase(Movie.UNKNOWN)) {
                    if (!tempDirectors.isEmpty()) {
                        movie.setDirector(tempDirectors.get(0));
                    }
                }
            }

            if (movie.getReleaseDate().equals(Movie.UNKNOWN)) {
                movie.setReleaseDate(HTMLTools.extractTag(xml, "<h5>" + siteDef.getReleaseDate() + ":</h5>", 1));
            }

            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                movie.setRuntime(getPreferredValue(HTMLTools.extractTags(xml, "<h5>" + siteDef.getRuntime() + ":</h5>")));
            }

            if (movie.getCountry().equals(Movie.UNKNOWN)) {
                // HTMLTools.extractTags(xml, "<h5>" + siteDef.getCountry() + ":</h5>", "</div>", "<a href", "</a>")
                for (String country : HTMLTools.extractTags(xml, "<h5>" + siteDef.getCountry() + ":</h5>", "</div>")) {
                    if (country != null) {
                        // TODO Save more than one country
                        movie.setCountry(HTMLTools.removeHtmlTags(country));
                        break;
                    }
                }
            }

            if (movie.getCompany().equals(Movie.UNKNOWN)) {
                for (String company : HTMLTools.extractTags(xml, "<h5>" + siteDef.getCompany() + ":</h5>", "</div>", "<a href", "</a>")) {
                    if (company != null) {
                        // TODO Save more than one company
                        movie.setCompany(company);
                        break;
                    }
                }
            }

            if (movie.getGenres().isEmpty()) {
                for (String genre : HTMLTools.extractTags(xml, "<h5>" + siteDef.getGenre() + ":</h5>", "</div>")) {
                    genre = HTMLTools.removeHtmlTags(genre);
                    if (genre.toLowerCase().endsWith("more")) {
                        genre = genre.substring(0, genre.length() - 4).trim();
                    }
                    int pos = genre.toLowerCase().indexOf("see more");
                    if (pos > 0) {
                        genre = genre.substring(0, pos).trim();
                    }
                    movie.addGenre(Library.getIndexingGenre(genre));
                }
            }

            if (movie.getQuote().equals(Movie.UNKNOWN)) {
                for (String quote : HTMLTools.extractTags(xml, "<h5>" + siteDef.getQuotes() + ":</h5>", "</div>", "<a href=\"/name/nm", "</a class=\"")) {
                    if (quote != null) {
                        quote = HTMLTools.stripTags(quote);
                        if (quote.endsWith("more")) {
                            quote = quote.substring(0, quote.length() - 4);
                        }
                        movie.setQuote(quote);
                        break;
                    }
                }
            }

            String imdbOutline = Movie.UNKNOWN;
            int plotBegin = xml.indexOf(("<h5>" + siteDef.getPlot() + ":</h5>"));
            if (plotBegin > -1) {
                plotBegin += ("<h5>" + siteDef.getPlot() + ":</h5>").length();
                // search "<a " for the international variety of "more" oder "add synopsis"
                int plotEnd = xml.indexOf("<a ", plotBegin);
                int plotEndOther = xml.indexOf("</a>", plotBegin);
                if (plotEnd > -1 || plotEndOther > -1) {
                    if ((plotEnd > -1 && plotEndOther < plotEnd) || plotEnd == -1) {
                        plotEnd = plotEndOther;
                    }

                    String outline = HTMLTools.stripTags(xml.substring(plotBegin, plotEnd)).trim();
                    if (outline.length() > 0) {
                        if (outline.endsWith("|")) {
                            // Remove the bar character from the end of the plot
                            outline = outline.substring(0, outline.length() - 1);
                        }

                        imdbOutline = outline.trim();
                        if (imdbOutline.length() > preferredPlotLength) {
                            imdbOutline = imdbOutline.substring(0, Math.min(imdbOutline.length(), preferredPlotLength - 3)) + "...";
                        }

                    }
                }
            }

            if (movie.getOutline().equals(Movie.UNKNOWN)) {
                movie.setOutline(imdbOutline);
            }

            if (movie.getPlot().equals(Movie.UNKNOWN)) {
                String plot = Movie.UNKNOWN;
                if (imdbPlot.equalsIgnoreCase("long")) {
                    plot = getLongPlot(movie);
                }

                // even if "long" is set we will default to the "short" one if none was found
                if (plot.equals(Movie.UNKNOWN)) {
                    plot = imdbOutline;
                }

                movie.setPlot(plot);
            }

            String certification = movie.getCertification();
            if (certification.equals(Movie.UNKNOWN)) {
                if (extractCertificationFromMPAA) {
                    String mpaa = HTMLTools.extractTag(xml, "<h5><a href=\"/mpaa\">MPAA</a>:</h5>", 1);
                    if (!mpaa.equals(Movie.UNKNOWN)) {
                        String key = siteDef.getRated() + " ";
                        int pos = mpaa.indexOf(key);
                        if (pos != -1) {
                            int start = key.length();
                            pos = mpaa.indexOf(" on appeal for ", start);
                            if (pos == -1) {
                                pos = mpaa.indexOf(" for ", start);
                            }
                            if (pos != -1) {
                                certification = mpaa.substring(start, pos);
                            }
                        }
                    }
                }
                if (certification.equals(Movie.UNKNOWN)) {
                    certification = getPreferredValue(HTMLTools.extractTags(xml, "<h5>" + siteDef.getCertification() + ":</h5>", "</div>",
                                    "<a href=\"/search/title?certificates=", "</a>"));
                }
                if (certification == null || certification.equalsIgnoreCase(Movie.UNKNOWN)) {
                    certification = Movie.NOTRATED;
                }
                movie.setCertification(certification);
            }

            // get year of imdb site
            if (!movie.isOverrideYear()) {
                Pattern getYear = Pattern.compile("(?:\\s*" + "\\((\\d{4})(?:/[^\\)]+)?\\)|<a href=\"/year/(\\d{4}))");
                Matcher m = getYear.matcher(xml);
                if (m.find()) {
                    String Year = m.group(1);
                    if (Year == null || Year.isEmpty()) {
                        Year = m.group(2);
                    }

                    if (Year != null && !Year.isEmpty()) {
                        movie.setYear(Year);
                    }
                }
            }

            if (!movie.isOverrideYear() && (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN))) {
                movie.setYear(HTMLTools.extractTag(xml, "<a href=\"/year/", 1));
                if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                    String fullReleaseDate = HTMLTools.getTextAfterElem(xml, "<h5>" + siteDef.getOriginalAirDate() + ":</h5>", 0);
                    if (!fullReleaseDate.equalsIgnoreCase(Movie.UNKNOWN)) {
                        movie.setYear(fullReleaseDate.split(" ")[2]);
                    }
                    // HTMLTools.extractTag(xml, "<h5>" + siteDef.getOriginal_air_date() + ":</h5>", 2, " ")
                }
            }

            if (movie.getCast().isEmpty()) {
                movie.setCast(HTMLTools.extractTags(xml, "<table class=\"cast\">", "</table>", "<td class=\"nm\"><a href=\"/name/", "</a>"));
            }

            /** Check for writer(s) **/
            if (movie.getWriters().isEmpty()) {
                movie.setWriters(HTMLTools.extractTags(xml, "<h5>" + siteDef.getWriter(), "</div>", "<a href=\"/name/", "</a>"));
            }
            // Removing Poster info from plugins. Use of PosterScanner routine instead.

            // if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            // movie.setPosterURL(locatePosterURL(movie, xml));
            // }

            if (movie.isTVShow()) {
                updateTVShowInfo(movie);
            }

            // TODO: Remove this check at some point when all skins have moved over to the new property
            downloadFanart = checkDownloadFanart(movie.isTVShow());

            if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
                movie.setFanartURL(getFanartURL(movie));
                if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                }
            }
        } catch (Exception error) {
            logger.severe("Failed retreiving IMDb data for movie : " + movie.getId(IMDB_PLUGIN_ID));
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return true;
    }

    private int parseRating(String rating) {
        StringTokenizer st = new StringTokenizer(rating, "/ ()");
        try {
            return (int)(Float.parseFloat(st.nextToken()) * 10);
        } catch (Exception error) {
            return -1;
        }
    }

    /**
     * Get the fanart for the movie from the FanartScanner
     * 
     * @param movie
     * @return
     */
    protected String getFanartURL(Movie movie) {
        return FanartScanner.getFanartURL(movie);
    }

    // /**
    // * Locate the poster URL from online sources
    // *
    // * @param movie
    // * Movie bean for the video to locate
    // * @param imdbXML
    // * XML page from IMDB to search for a URL
    // * @return The URL of the poster if found.
    // */
    // protected String locatePosterURL(Movie movie, String imdbXML) {
    // return PosterScanner.getPosterURL(movie, imdbXML, IMDB_PLUGIN_ID);
    // }

    @Override
    public void scanTVShowTitles(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            return;
        }

        try {
            String xml = webBrowser.request(siteDef.getSite() + "title/" + imdbId + "/episodes");
            int season = movie.getSeason();
            for (MovieFile file : movie.getMovieFiles()) {
                if (!file.isNewFile() || file.hasTitle()) {
                    // don't scan episode title if it exists in XML data
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (int episode = file.getFirstPart(); episode <= file.getLastPart(); ++episode) {
                    String episodeName = HTMLTools.extractTag(xml, "Season " + season + ", Episode " + episode + ":", 2);

                    if (!episodeName.equals(Movie.UNKNOWN) && episodeName.indexOf("Episode #") == -1) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(" / ");
                        }
                        sb.append(episodeName);
                    }
                }
                String title = sb.toString();
                if (!"".equals(title)) {
                    file.setTitle(title);
                }
            }
        } catch (IOException error) {
            logger.severe("Failed retreiving episodes titles for movie : " + movie.getTitle());
            logger.severe("Error : " + error.getMessage());
        }
    }

    /**
     * Get the TV show information from IMDb
     * 
     * @throws IOException
     * @throws MalformedURLException
     */
    protected void updateTVShowInfo(Movie movie) throws MalformedURLException, IOException {
        scanTVShowTitles(movie);
    }

    /**
     * Retrieves the long plot description from IMDB if it exists, else "None"
     * 
     * @param movie
     * @return long plot
     */
    private String getLongPlot(Identifiable movie) {
        String plot = Movie.UNKNOWN;

        try {
            String xml = webBrowser.request(siteDef.getSite() + "title/" + movie.getId(IMDB_PLUGIN_ID) + "/plotsummary", siteDef.getCharset());

            String result = HTMLTools.extractTag(xml, "<p class=\"plotpar\">");
            if (!result.equalsIgnoreCase(Movie.UNKNOWN) && result.indexOf("This plot synopsis is empty") < 0) {
                plot = result;
            }

            // Second parsing other site (fr/ es / ect ...)
            result = HTMLTools.getTextAfterElem(xml, "<div id=\"swiki.2.1\">");
            if (!result.equalsIgnoreCase(Movie.UNKNOWN) && result.indexOf("This plot synopsis is empty") < 0) {
                plot = result;
            }

        } catch (Exception error) {
            plot = Movie.UNKNOWN;
        }

        if (plot.length() > preferredPlotLength) {
            plot = plot.substring(0, Math.min(plot.length(), preferredPlotLength - 3)) + "...";
        }

        return plot;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.finest("Scanning NFO for Imdb Id");
        String id = searchIMDB(nfo, movie);
        if (id != null) {
            movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, id);
            logger.finer("Imdb Id found in nfo: " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        } else {
            int beginIndex = nfo.indexOf("/tt");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
                logger.finer("Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
            } else {
                beginIndex = nfo.indexOf("/Title?");
                if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                    StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                    movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
                } else {
                    logger.finer("No Imdb Id found in nfo !");
                }
            }
        }
    }

    private String searchIMDB(String nfo, Movie movie) {
        final int flags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
        String imdbPattern = ")[\\W].*?(tt\\d{7})";
        String title = movie.getTitle();
        String id = null;

        Pattern patternTitle = Pattern.compile("(" + title + imdbPattern, flags);
        Matcher matchTitle = patternTitle.matcher(nfo);
        if (matchTitle.find()) {
            id = matchTitle.group(2);
        } else {
            String dir = FileTools.getParentFolderName(movie.getFile());
            Pattern patternDir = Pattern.compile("(" + dir + imdbPattern, flags);
            Matcher matchDir = patternDir.matcher(nfo); 
            if (matchDir.find()) {
                id = matchDir.group(2);
            } else {
                String strippedNfo = nfo.replaceAll("(?is)[^\\w\\r\\n]", "");
                String strippedTitle = title.replaceAll("(?is)[^\\w\\r\\n]", "");
                Pattern patternStrippedTitle = Pattern.compile("(" + strippedTitle + imdbPattern, flags);
                Matcher matchStrippedTitle = patternStrippedTitle.matcher(strippedNfo);
                if (matchStrippedTitle.find()) {
                    id = matchTitle.group(2);
                } else {
                    String strippedDir = dir.replaceAll("(?is)[^\\w\\r\\n]", "");
                    Pattern patternStrippedDir = Pattern.compile("(" + strippedDir + imdbPattern, flags);
                    Matcher matchStrippedDir = patternStrippedDir.matcher(strippedNfo);
                    if (matchStrippedDir.find()) {
                        id = matchTitle.group(2);
                    }
                }
            }
        }
        return id;
    }

    /**
     * Checks for older fanart property in case the skin hasn't been updated. TODO: Remove this procedure at some point
     * 
     * @return true if the fanart is to be downloaded, or false otherwise
     */
    public static boolean checkDownloadFanart(boolean isTvShow) {
        String fanartProperty = null;
        boolean downloadFanart = false;

        if (isTvShow) {
            fanartProperty = PropertiesUtil.getProperty("fanart.tv.download");
        } else {
            fanartProperty = PropertiesUtil.getProperty("fanart.movie.download");
        }

        // If this is null, then the property wasn't found, so look for the original
        if (fanartProperty == null) {
            downloadFanart = Boolean.parseBoolean(PropertiesUtil.getProperty("moviedb.fanart.download", "false"));
        } else {
            downloadFanart = Boolean.parseBoolean(fanartProperty);
        }

        return downloadFanart;
    }

}
