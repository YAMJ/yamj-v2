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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
    protected ImdbSiteDataDefinition siteDef;
    protected ImdbInfo imdbInfo;

    public ImdbPlugin() {
        imdbInfo = new ImdbInfo();
        siteDef = imdbInfo.getSiteDef();

        webBrowser = new WebBrowser();

        // PropertiesUtil.getProperty("imdb.id.search", "imdb");
        // Boolean.parseBoolean(PropertiesUtil.getProperty("imdb.perfect.match", "true"));
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
        String imdbID = movie.getId(IMDB_PLUGIN_ID);
        boolean imdbNewVersion = false; // Used to fork the processing for the new version of IMDb
        boolean returnStatus = false;
        
        try {
            if (!imdbID.startsWith("tt")) {
                imdbID = "tt" + imdbID;
                // Correct the ID if it's wrong
                movie.setId(IMDB_PLUGIN_ID, "tt" + imdbID);
            }
            
            String xml = getImdbUrl(movie);
            
            // Add the combined tag to the end of the request if required
            if (getFullInfo) {
                xml += "combined";
            }
            
            xml = webBrowser.request(xml, siteDef.getCharset());
            
            if (xml.contains("\"tv-extra\"")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            // We can work out if this is the new site by looking for " - IMDb" at the end of the title
            String title = HTMLTools.extractTag(xml, "<title>");
            title = title.replaceAll(" \\([VG|V]\\)$", ""); // Remove the (VG) or (V) tags from the title
            title = title.replaceAll(" \\((\\d{4})(?:/[^\\)]+)?\\)", ""); // Remove the Date identifier
            
            // Check for the new version and correct the title if found.
            if (title.toLowerCase().endsWith(" - imdb")) {
                title = title.substring(0, title.length() - 7);
                imdbNewVersion = true;
            } else {
                imdbNewVersion = false;
            }
                
            if (!movie.isOverrideTitle()) {
                movie.setTitle(title);
                movie.setOriginalTitle(title);
            }
            
            if (imdbNewVersion) {
                returnStatus = updateInfoNew(movie, xml);
            } else {
                returnStatus = updateInfoOld(movie, xml);
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
        return returnStatus;
    }

    /**
     * Process the old IMDb formatted web page
     * @param movie
     * @param xml
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private boolean updateInfoOld(Movie movie, String xml) throws MalformedURLException, IOException {
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
            // Issue 1261 : Allow multiple text matching for one "element".
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
                movie.addGenre(Library.getIndexingGenre(cleanSeeMore(genre)));
            }
        }

        if (movie.getQuote().equals(Movie.UNKNOWN)) {
            for (String quote : HTMLTools.extractTags(xml, "<h5>" + siteDef.getQuotes() + ":</h5>", "</div>", "<a href=\"/name/nm", "</a class=\"")) {
                if (quote != null) {
                    quote = HTMLTools.stripTags(quote);
                    movie.setQuote(cleanSeeMore(quote));
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
                    if (FileTools.isValidString(imdbOutline)) {
                        if (imdbOutline.length() > preferredPlotLength) {
                            imdbOutline = imdbOutline.substring(0, Math.min(imdbOutline.length(), preferredPlotLength - 3)) + "...";
                        }
                    } else {
                        // Ensure the outline isn't blank or null
                        imdbOutline = Movie.UNKNOWN;
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

        // Get year of movie from IMDb site
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
        
        return true;
    }
    
    /**
     * Process the new IMDb format web page
     * @param movie
     * @param xml
     * @return
     * @throws MalformedURLException
     * @throws IOException
     */
    private boolean updateInfoNew(Movie movie, String xml) throws MalformedURLException, IOException {
        logger.finer("ImdbPlugin: Detected new IMDb format for '" + movie.getBaseName() + "'");
        Collection<String> peopleList;
        String releaseInfoXML = Movie.UNKNOWN;  // Store the release info page for release info & AKAs

        // Overwrite the normal siteDef with a v2 siteDef if it exists
        ImdbSiteDataDefinition siteDef = imdbInfo.getSiteDef(imdbInfo.getImdbsite() + "2");
        if (siteDef == null) {
            logger.severe("ImdbPlugin: No new format definition found for language '" + imdbInfo.getImdbsite() + "'");
            logger.severe("ImdbPlugin: No data will be scraped for this movie");
            return false;
        }
        
        // RATING (Working)
        if (movie.getRating() == -1) {
            movie.setRating(parseRating(HTMLTools.extractTag(xml, "star-bar-user-rate\">", 1).replace(",", ".")));
        }

        // TOP250 (Working)
        if (movie.getTop250() == -1) {
            try {
                movie.setTop250(Integer.parseInt(HTMLTools.extractTag(xml, "Top 250 #")));
            } catch (NumberFormatException error) {
                movie.setTop250(-1);
            }
        }

        // RELEASE DATE (Working)
        if (movie.getReleaseDate().equals(Movie.UNKNOWN)) {
            // Load the release page from IMDb
            if (releaseInfoXML.equals(Movie.UNKNOWN)) {
                releaseInfoXML = webBrowser.request(getImdbUrl(movie) + "releaseinfo", siteDef.getCharset());
            }
            movie.setReleaseDate(HTMLTools.stripTags(HTMLTools.extractTag(releaseInfoXML, "\">" + preferredCountry, "</a></td>")).trim());
        }

        // RUNTIME (Working)
        if (movie.getRuntime().equals(Movie.UNKNOWN)) {
            String runtime = siteDef.getRuntime() + ":</h4>";
            ArrayList<String> runtimes = HTMLTools.extractTags(xml, runtime, "</div>", null, "|", false);
            runtime = getPreferredValue(runtimes);

            // Strip any extraneous characters from the runtime
            int pos = runtime.indexOf("min");
            if (pos > 0) {
                runtime = runtime.substring(0, pos + 3);
            }
            movie.setRuntime(runtime);
        }

        // COUNTRY (Working)
        if (movie.getCountry().equals(Movie.UNKNOWN)) {
            for (String country : HTMLTools.extractTags(xml, siteDef.getCountry() + ":</h4>", "</div>", "<a href", "</a>")) {
                if (country != null) {
                    // TODO Save more than one country
                    movie.setCountry(HTMLTools.removeHtmlTags(country));
                    break;
                }
            }
        }

        // COMPANY (Changed)
        if (movie.getCompany().equals(Movie.UNKNOWN)) {
            for (String company : HTMLTools.extractTags(xml, siteDef.getCompany() + ":</h4>", "<span class", "<a ", "</a>")) {
                if (company != null) {
                    // TODO Save more than one company
                    movie.setCompany(company);
                    break;
                }
            }
        }

        // GENRES (Working)
        if (movie.getGenres().isEmpty()) {
            for (String genre : HTMLTools.extractTags(xml, siteDef.getGenre() + ":</h4>", "</div>", "<a href=\"", "</a>")) {
                movie.addGenre(Library.getIndexingGenre(HTMLTools.removeHtmlTags(genre)));
            }
        }

        // QUOTE (Working)
        if (movie.getQuote().equals(Movie.UNKNOWN)) {
            for (String quote : HTMLTools.extractTags(xml, "<h4>" + siteDef.getQuotes() + "</h4>", "<span class=\"", "<a ", "<br")) {
                if (quote != null) {
                    quote = HTMLTools.stripTags(quote);
                    movie.setQuote(cleanSeeMore(quote));
                    break;
                }
            }
        }

        // OUTLINE (Working)
        if (movie.getOutline().equals(Movie.UNKNOWN)) {
            // The new outline is at the end of the review section with no preceding text
            String imdbOutline = HTMLTools.extractTag(xml, "reviews</a></span>", "<div class=\"txt-block\">");
            imdbOutline = HTMLTools.removeHtmlTags(imdbOutline).trim();
            
            if (FileTools.isValidString(imdbOutline)) {
                if (imdbOutline.length() > preferredPlotLength) {
                    imdbOutline = imdbOutline.substring(0, Math.min(imdbOutline.length(), preferredPlotLength - 3)) + "...";
                }
            } else {
                // ensure the outline is set to unknown if it's blank or null
                imdbOutline = Movie.UNKNOWN;
            }
            movie.setOutline(imdbOutline);
        }
        
        // PLOT (Working)
        if (movie.getPlot().equals(Movie.UNKNOWN)) {
            // The new plot is now called Storyline
            String imdbPlot = HTMLTools.extractTag(xml, "<h2>" + siteDef.getPlot() + "</h2>", "<em class=\"nobr\">");
            imdbPlot = HTMLTools.removeHtmlTags(imdbPlot).trim();
            
            // This plot didn't work, look for another version
            if (!FileTools.isValidString(imdbPlot)) {
                imdbPlot = HTMLTools.extractTag(xml, "<h2>" + siteDef.getPlot() + "</h2>", "<span class=\"");
                imdbPlot = HTMLTools.removeHtmlTags(imdbPlot).trim();
            }
            
            // Check the length of the plot is OK
            if (FileTools.isValidString(imdbPlot)) {
                if  (imdbPlot.length() > preferredPlotLength) {
                    imdbPlot = imdbPlot.substring(0, Math.min(imdbPlot.length(), preferredPlotLength - 3)) + "...";
                }
            } else {
                // The plot might be blank or null so set it to UNKNOWN
                imdbPlot = Movie.UNKNOWN;
            }
            
            // Update the plot with the found plot, or the outline if not found
            if (FileTools.isValidString(imdbPlot)) {
                movie.setPlot(imdbPlot);
            } else {
                movie.setPlot(movie.getOutline());
            }
        }
        
        // CERTIFICATION (Working)
        String certification = movie.getCertification();
        if (certification.equals(Movie.UNKNOWN)) {
            String certXML = webBrowser.request(getImdbUrl(movie) + "parentalguide#certification", siteDef.getCharset());
            if (extractCertificationFromMPAA) {
                String mpaa = HTMLTools.extractTag(certXML, "<h5><a href=\"/mpaa\">MPAA</a>:</h5>", 1);
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
                certification = getPreferredValue(HTMLTools.extractTags(certXML, "<h5>" + siteDef.getCertification() + ":</h5>", "</div>",
                                "<a href=\"/search/title?certificates=", "</a>"));
            }
            if (certification == null || certification.equalsIgnoreCase(Movie.UNKNOWN)) {
                certification = Movie.NOTRATED;
            }
            movie.setCertification(certification);
        }

        // Get year of IMDb site (Working)
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

        // CAST
        if (movie.getCast().isEmpty()) {
            peopleList = HTMLTools.extractTags(xml, "<table class=\"cast_list\">", "</table>", "<td class=\"name\"", "</td>"); 
            String castMember;
            
            if (peopleList.isEmpty()) {
                // Try an alternative search
                peopleList = HTMLTools.extractTags(xml, "<table class=\"cast_list\">", "</table>", "/name/nm", "</a>", true);
            }
            
            // Clean up the cast list that is returned
            for (Iterator<String> iter = peopleList.iterator(); iter.hasNext();) {
                castMember = iter.next();
                if (castMember.indexOf("src=") > -1) {
                    iter.remove();
                } else {
                    // Add the cleaned up cast member to the movie
                    movie.addActor(HTMLTools.stripTags(castMember));
                }
            }
        }

        // DIRECTOR(S) (Working)
        if (movie.getDirector().equals(Movie.UNKNOWN)) {
            peopleList = parseNewPeople(xml, siteDef.getDirector().split("\\|"));
            movie.setDirector(peopleList);
        }

        // WRITER(S)
        if (movie.getWriters().isEmpty()) {
            peopleList = parseNewPeople(xml, siteDef.getWriter().split("\\|")); 
            String writer;
            
            for (Iterator<String> iter = peopleList.iterator(); iter.hasNext();) {
                writer = iter.next();
                // Clean up by removing the phrase "and ? more credits"
                if (writer.indexOf("more credit") == -1) {
                    movie.addWriter(writer);
                }
            }
        }
        
        // ORIGINAL TITLE / AKAS

        // Load the AKA page from IMDb
        if (releaseInfoXML.equals(Movie.UNKNOWN)) {
            releaseInfoXML = webBrowser.request(getImdbUrl(movie) + "releaseinfo", siteDef.getCharset());
        }
        
        // The AKAs are stored in the format "title", "country"
        // therefore we need to look for the preferredCountry and then work backwards
        
        // Just extract the AKA section from the page
        ArrayList<String> akaList = HTMLTools.extractTags(releaseInfoXML, "Also Known As (AKA)", "</table>", "<td>", "</td>", false);

        // Does the "original title" exist on the page?
        if (akaList.toString().indexOf("original title") > 0) {
            // This table comes back as a single list, so we have to save the last entry in case it's the one we need
            String previousEntry = "";
            boolean foundAka = false;
            for (String akaTitle : akaList) {
                if (akaTitle.indexOf("original title") == -1) {
                    // We've found the entry, so quit
                    foundAka = true;
                    break;
                } else {
                    previousEntry = akaTitle;
                }
            }
            
            if (foundAka) {
                movie.setOriginalTitle(HTMLTools.stripTags(previousEntry).trim());
            }
        }
        
        // TV SHOW
        if (movie.isTVShow()) {
            updateTVShowInfo(movie);
        }

        return true;
    }
    
    /**
     * Process a list of people in the source XML
     * @param sourceXml
     * @param singleCategory    The singular version of the category, e.g. "Writer"
     * @param pluralCategory    The plural version of the category, e.g. "Writers"
     * @return
     */
    private Collection<String> parseNewPeople(String sourceXml, String[] categoryList) {
        Collection<String> people = new LinkedHashSet<String>();
        
        for (String category : categoryList) {
            if (sourceXml.indexOf(category + ":") >= 0) {
                people = HTMLTools.extractTags(sourceXml, category, "</div>", "<a ", "</a>");
            }
        }
        return people;
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

    protected static String cleanSeeMore(String uncleanString) {
        int pos = uncleanString.indexOf("more");
        
        // First let's check if "more" exists in the string
        if (pos > 0) {
            if (uncleanString.endsWith("more")) {
                return uncleanString.substring(0, uncleanString.length() - 4);
            }

            pos = uncleanString.toLowerCase().indexOf("see more");
            if (pos > 0) {
                return uncleanString.substring(0, pos).trim();
            }
        } else {
            return uncleanString.trim();
        }
        
        return uncleanString;
    }

    protected String getImdbUrl(Movie movie) {
        return siteDef.getSite() + "title/" + movie.getId(IMDB_PLUGIN_ID) + "/";
    }
}
