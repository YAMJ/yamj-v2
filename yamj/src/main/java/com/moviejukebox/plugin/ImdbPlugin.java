/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin;

import static com.moviejukebox.model.Movie.UNKNOWN;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.tools.StringTools.trimToLength;

import com.moviejukebox.model.*;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImdbPlugin implements MovieDatabasePlugin {

    public static final String IMDB_PLUGIN_ID = "imdb";
    private static final Logger LOG = LoggerFactory.getLogger(ImdbPlugin.class);
    protected String preferredCountry;
    private final String imdbPlot;
    protected YamjHttpClient httpClient;
    protected boolean downloadFanart;
    private final boolean extractCertificationFromMPAA;
    private final boolean fullInfo;
    protected String fanartToken;
    protected String fanartExtension;
    private final int preferredBiographyLength;
    private final int preferredFilmographyMax;
    protected int actorMax;
    protected int directorMax;
    protected int writerMax;
    private final int triviaMax;
    protected ImdbInfo imdbInfo;
    protected AspectRatioTools aspectTools;
    private final boolean skipFaceless;
    private final boolean skipVG;
    private final boolean skipTV;
    private final boolean skipV;
    private final List<String> jobsInclude;
    // Should we scrape the award information
    private final boolean scrapeAwards;
    // Should we scrape the won awards only
    private final boolean scrapeWonAwards;
    // Should we scrape the business information
    private final boolean scrapeBusiness;
    // Should we scrape the trivia information
    private final boolean scrapeTrivia;
    // Site Literals
    private static final String IMDB_TITLE = "title/";
    private static final String IMDB_NAME = "name/";
    // Suffix Literals
    private static final String SUFFIX_FILMOYEAR = "/filmoyear";
    private static final String SUFFIX_BIO = "/bio";
    private static final String SUFFIX_PARENTALGUIDE = "/parentalguide#certification";
    private static final String SUFFIX_RELEASEINFO = "/releaseinfo";
    private static final String SUFFIX_AWARDS = "/awards";
    private static final String SUFFIX_FULLCREDITS = "/fullcredits";
    private static final String SUFFIX_PLOTSUMMARY = "/plotsummary";
    private static final String SUFFIX_TRIVIA = "/trivia";
    private static final String SUFFIX_BUSINESS = "/business";
    // Literals
    private static final String HTML_H5_END = ":</h5>";
    private static final String HTML_H5_START = "<h5>";
    private static final String HTML_DIV_END = "</div>";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_A_START = "<a ";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_TD_END = "</td>";
    private static final String HTML_H4_END = ":</h4>";
    private static final String HTML_BREAK = "<br/>";
    private static final String HTML_SPAN_END = "</span>";
    private static final String HTML_GT = ">";
    // Patterns for the name searching
    private static final Pattern PATTERN_BIO = Pattern.compile("<h\\d.*?>Mini Bio.*?</h\\d>.*?<p>(.*?)</p>");

    // Patterns for filmography
    // 1: Section title (job), 2: Number of credits
    private static final Pattern pJobSection = Pattern.compile("<a name=\".*?\">(.*?)</a> \\((\\d*?) credit.?\\)");
    // 1: Single video
    private static final Pattern pJobItems = Pattern.compile("(?s)(<div class=\"filmo-row.*?>(.*?)(?:</div>\\s*)+)");
    // 1: Release Year (for acting roles)
    private static final Pattern pJobYear = Pattern.compile("\\\"year_column\\\">(?:&nbsp;){0,1}(\\d{4})</span>");
    // 1: IMDB ID, 2: Title
    private static final Pattern pJobIdTitle = Pattern.compile("/title/(tt\\d*?)/.*?>(.*?)<br");

    // Pattern for DOB
    private static final Pattern PATTERN_DOB = Pattern.compile("(\\d{1,2})-(\\d{1,2})");

    // AKA scraping
    private final boolean akaScrapeTitle;
    private final String[] akaMatchingCountries;
    private final String[] akaIgnoreVersions;
    
    public ImdbPlugin() {
        imdbInfo = new ImdbInfo();
        aspectTools = new AspectRatioTools();

        httpClient = YamjHttpClientBuilder.getHttpClient();

        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
        imdbPlot = PropertiesUtil.getProperty("imdb.plot", "short");
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.movie.download", Boolean.FALSE);
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        fanartExtension = PropertiesUtil.getProperty("fanart.format", "jpg");
        extractCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", Boolean.TRUE);
        fullInfo = PropertiesUtil.getBooleanProperty("imdb.full.info", Boolean.FALSE);

        // People properties
        preferredBiographyLength = PropertiesUtil.getIntProperty("plugin.biography.maxlength", 500);
        preferredFilmographyMax = PropertiesUtil.getIntProperty("plugin.filmography.max", 20);
        actorMax = PropertiesUtil.getReplacedIntProperty("movie.actor.maxCount", "plugin.people.maxCount.actor", 10);
        directorMax = PropertiesUtil.getReplacedIntProperty("movie.director.maxCount", "plugin.people.maxCount.director", 2);
        writerMax = PropertiesUtil.getReplacedIntProperty("movie.writer.maxCount", "plugin.people.maxCount.writer", 3);
        skipFaceless = PropertiesUtil.getBooleanProperty("plugin.people.skip.faceless", Boolean.FALSE);
        skipVG = PropertiesUtil.getBooleanProperty("plugin.people.skip.VG", Boolean.TRUE);
        skipTV = PropertiesUtil.getBooleanProperty("plugin.people.skip.TV", Boolean.FALSE);
        skipV = PropertiesUtil.getBooleanProperty("plugin.people.skip.V", Boolean.FALSE);
        jobsInclude = Arrays.asList(PropertiesUtil.getProperty("plugin.filmography.jobsInclude", "Director,Writer,Actor,Actress").split(","));

        // Trivia properties
        triviaMax = PropertiesUtil.getIntProperty("plugin.trivia.maxCount", 15);

        // Award properties
        String tmpAwards = PropertiesUtil.getProperty("mjb.scrapeAwards", FALSE);
        scrapeWonAwards = tmpAwards.equalsIgnoreCase("won");
        scrapeAwards = tmpAwards.equalsIgnoreCase(TRUE) || scrapeWonAwards;

        // Business properties
        scrapeBusiness = PropertiesUtil.getBooleanProperty("mjb.scrapeBusiness", Boolean.FALSE);

        // Trivia properties
        scrapeTrivia = PropertiesUtil.getBooleanProperty("mjb.scrapeTrivia", Boolean.FALSE);

        // Other properties
        akaScrapeTitle = PropertiesUtil.getBooleanProperty("imdb.aka.scrape.title", Boolean.FALSE);
        akaIgnoreVersions = PropertiesUtil.getProperty("imdb.aka.ignore.versions", "").split(",");

        String fallbacks = PropertiesUtil.getProperty("imdb.aka.fallback.countries", "");
        if (StringTools.isNotValidString(fallbacks)) {
            akaMatchingCountries = new String[]{preferredCountry};
        } else {
            akaMatchingCountries = (preferredCountry + "," + fallbacks).split(",");
        }
    }

    @Override
    public String getPluginID() {
        return IMDB_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (isNotValidString(imdbId)) {
            imdbId = imdbInfo.getImdbId(movie.getTitle(), movie.getYear(), movie.isTVShow());
            movie.setId(IMDB_PLUGIN_ID, imdbId);
        }

        boolean retval = Boolean.FALSE;
        if (isValidString(imdbId)) {
            retval = updateImdbMediaInfo(movie);
        }
        return retval;
    }

    protected String getPreferredValue(List<String> values, boolean useLast) {
        String value = UNKNOWN;

        if (useLast) {
            Collections.reverse(values);
        }

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
                if (value.equals(UNKNOWN)) {
                    value = text;
                }
            } else {
                if (country.equals(preferredCountry)) {
                    value = text;
                    // No need to continue scanning
                    break;
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
        if (!imdbID.startsWith("tt")) {
            imdbID = "tt" + imdbID;
            // Correct the ID if it's wrong
            movie.setId(IMDB_PLUGIN_ID, imdbID);
        }

        String xml = ImdbPlugin.this.getImdbUrl(movie);

        // Add the combined tag to the end of the request if required
        if (fullInfo) {
            xml += "combined";
        }

        xml = getImdbData(xml);

        if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW) && (xml.contains("\"tv-extra\"") || xml.contains("\"tv-series-series\""))) {
            movie.setMovieType(Movie.TYPE_TVSHOW);
            return Boolean.FALSE;
        }

        // We can work out if this is the new site by looking for " - IMDb" at the end of the title
        String title = HTMLTools.extractTag(xml, "<title>");
        if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW) && title.contains("(TV Series")) {
            movie.setMovieType(Movie.TYPE_TVSHOW);
            return Boolean.FALSE;
        }

        // Correct the title if "imdb" found
        if (StringUtils.endsWithIgnoreCase(title, " - imdb")) {
            title = title.substring(0, title.length() - 7);
        } else if (StringUtils.startsWithIgnoreCase(title, "imdb - ")) {
            title = title.substring(7);
        }

        // Remove the (VG) or (V) tags from the title
        title = title.replaceAll(" \\([VG|V]\\)$", "");

        //String yearPattern = "(?i).\\((?:TV.|VIDEO.)?(\\d{4})(?:/[^\\)]+)?\\)";
        String yearPattern = "(?i).\\((?:TV.|VIDEO.)?(\\d{4})";
        Pattern pattern = Pattern.compile(yearPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(title);
        if (matcher.find()) {
            // If we've found a year, set it in the movie
            if (OverrideTools.checkOverwriteYear(movie, IMDB_PLUGIN_ID)) {
                movie.setYear(matcher.group(1), IMDB_PLUGIN_ID);
            }

            // Remove the year from the title
            title = title.substring(0, title.indexOf(matcher.group(0)));
        }

        if (OverrideTools.checkOverwriteTitle(movie, IMDB_PLUGIN_ID)) {
            movie.setTitle(title, IMDB_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteOriginalTitle(movie, IMDB_PLUGIN_ID)) {
            String originalTitle = title;
            if (xml.contains("<span class=\"title-extra\">")) {
                originalTitle = HTMLTools.extractTag(xml, "<span class=\"title-extra\">", "</span>");
                if (originalTitle.contains("(original title)")) {
                    originalTitle = originalTitle.replace(" <i>(original title)</i>", "");
                } else {
                    originalTitle = title;
                }
            }
            movie.setOriginalTitle(originalTitle, IMDB_PLUGIN_ID);
        }

        // Update the movie information
        updateInfo(movie, xml);

        // update common values
        updateInfoCommon(movie, xml);

        if (scrapeAwards) {
            updateAwards(movie);        // Issue 1901: Awards
        }

        if (scrapeBusiness) {
            updateBusiness(movie);      // Issue 2012: Financial information about movie
        }

        if (scrapeTrivia) {
            updateTrivia(movie);        // Issue 2013: Add trivia
        }

        // TODO: Move this check out of here, it doesn't belong.
        if (downloadFanart && isNotValidString(movie.getFanartURL())) {
            movie.setFanartURL(getFanartURL(movie));
            if (isValidString(movie.getFanartURL())) {
                movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
            }
        }

        // always true
        return Boolean.TRUE;
    }

    /**
     * Process the new IMDb format web page
     *
     * @param movie
     * @param xml
     */
    private void updateInfo(Movie movie, String xml) {
        // RATING
        if (movie.getRating(IMDB_PLUGIN_ID) == -1) {
            String srtRating = HTMLTools.extractTag(xml, "starbar-meta\">", HTML_DIV_END).replace(",", ".");
            int intRating = parseRating(HTMLTools.stripTags(srtRating));

            // Try another format for the rating
            if (intRating == -1) {
                srtRating = HTMLTools.extractTag(xml, "ratingValue\">", HTML_SPAN_END).replace(",", ".");
                intRating = parseRating(HTMLTools.stripTags(srtRating));
            }

            movie.addRating(IMDB_PLUGIN_ID, intRating);
        }

        // TOP250
        if (OverrideTools.checkOverwriteTop250(movie, IMDB_PLUGIN_ID)) {
            String top250;
            if (fullInfo) {
                top250 = HTMLTools.extractTag(xml, "Top 250: #");
            } else {
                top250 = HTMLTools.extractTag(xml, "Top Rated Movies #");
            }
            movie.setTop250(top250, IMDB_PLUGIN_ID);
        }

        // RUNTIME
        if (OverrideTools.checkOverwriteRuntime(movie, IMDB_PLUGIN_ID)) {
            String runtime = "Runtime" + HTML_H4_END;
            List<String> runtimes = HTMLTools.extractTags(xml, runtime, HTML_DIV_END, null, "|", Boolean.FALSE);
            runtime = getPreferredValue(runtimes, false);

            // Strip any extraneous characters from the runtime
            int pos = runtime.indexOf("min");
            if (pos > 0) {
                runtime = runtime.substring(0, pos + 3);
            }
            movie.setRuntime(runtime, IMDB_PLUGIN_ID);
        }

        // COUNTRY
        if (OverrideTools.checkOverwriteCountry(movie, IMDB_PLUGIN_ID)) {
            List<String> countries = new ArrayList<>();
            String startTag = "Country" + HTML_H5_END;
            if (!xml.contains(startTag)) {
                startTag = "Country" + HTML_H4_END;
            }
            for (String country : HTMLTools.extractTags(xml, startTag, HTML_DIV_END, "<a href=\"", HTML_A_END)) {
                countries.add(HTMLTools.removeHtmlTags(country));
            }
            movie.setCountries(countries, IMDB_PLUGIN_ID);
        }

        // COMPANY
        if (OverrideTools.checkOverwriteCompany(movie, IMDB_PLUGIN_ID)) {
            String startTag = "Company" + HTML_H5_END;
            if (!xml.contains(startTag)) {
                startTag = "Production Co" + HTML_H4_END;
            }
            if (!xml.contains(startTag)) {
                startTag = "<h3>Company";
            }

            for (String company : HTMLTools.extractTags(xml, startTag, HTML_DIV_END, "<a href", HTML_A_END)) {
                company = HTMLTools.stripTags(company, false);
                if (company != null) {
                    // TODO Save more than one company
                    movie.setCompany(company, IMDB_PLUGIN_ID);
                    break;
                }
            }
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(movie, IMDB_PLUGIN_ID)) {
            String startTag = "Genre" + HTML_H5_END;
            if (!xml.contains(startTag)) {
                startTag = "Genres" + HTML_H4_END;
            }
            movie.setGenres(HTMLTools.extractTags(xml, startTag, HTML_DIV_END, "<a href", HTML_A_END), IMDB_PLUGIN_ID);
        }

        // QUOTE
        if (OverrideTools.checkOverwriteQuote(movie, IMDB_PLUGIN_ID)) {
            for (String quote : HTMLTools.extractTags(xml, "<h4>Quotes</h4>", "<span class=\"", "<br", "<br")) {
                if (quote != null) {
                    quote = HTMLTools.stripTags(quote);
                    movie.setQuote(cleanStringEnding(quote), IMDB_PLUGIN_ID);
                    break;
                }
            }
        }

        // OUTLINE
        if (OverrideTools.checkOverwriteOutline(movie, IMDB_PLUGIN_ID)) {
            // The new outline is at the end of the review section with no preceding text
            String imdbOutline = HTMLTools.extractTag(xml, "<p itemprop=\"description\">", "</p>");
            imdbOutline = cleanStringEnding(HTMLTools.removeHtmlTags(imdbOutline)).trim();

            if (isNotValidString(imdbOutline)) {
                // ensure the outline is set to unknown if it's blank or null
                imdbOutline = UNKNOWN;
            }
            movie.setOutline(imdbOutline, IMDB_PLUGIN_ID);
        }

        // PLOT
        if (OverrideTools.checkOverwritePlot(movie, IMDB_PLUGIN_ID)) {
            getPlot(movie, xml);
        }

        // CERTIFICATION
        if (OverrideTools.checkOverwriteCertification(movie, IMDB_PLUGIN_ID)) {
            String certification = movie.getCertification();
            // Use the default site definition for the certification, because the local versions don't have the parentalguide page
            String certXML = getImdbData(getImdbUrl(movie, SUFFIX_PARENTALGUIDE));
            if (extractCertificationFromMPAA) {
                String mpaa = HTMLTools.extractTag(certXML, "<h5><a href=\"/mpaa\">MPAA</a>:</h5>", 1);
                if (!mpaa.equals(UNKNOWN)) {
                    String key = "Rated ";
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

            if (isNotValidString(certification)) {
                certification = getPreferredValue(HTMLTools.extractTags(certXML, HTML_H5_START + "Certification" + HTML_H5_END, HTML_DIV_END,
                        "<a href=\"/search/title?certificates=", HTML_A_END), true);
            }

            if (isNotValidString(certification)) {
                certification = Movie.NOTRATED;
            }

            movie.setCertification(certification, IMDB_PLUGIN_ID);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(movie, IMDB_PLUGIN_ID)) {
            Pattern getYear = Pattern.compile("(?:\\s*" + "\\((\\d{4})(?:/[^\\)]+)?\\)|<a href=\"/year/(\\d{4}))");
            Matcher m = getYear.matcher(xml);
            if (m.find()) {
                String year = m.group(1);
                if (isNotValidString(year)) {
                    year = m.group(2);
                }
                movie.setYear(year, IMDB_PLUGIN_ID);
            }

            // second approach
            if (isNotValidString(movie.getYear())) {
                movie.setYear(HTMLTools.extractTag(xml, "<a href=\"/year/", 1), IMDB_PLUGIN_ID);
                if (isNotValidString(movie.getYear())) {
                    String fullReleaseDate = HTMLTools.getTextAfterElem(xml, HTML_H5_START + "Original Air Date" + HTML_H5_END, 0);
                    if (isValidString(fullReleaseDate)) {
                        movie.setYear(fullReleaseDate.split(" ")[2], IMDB_PLUGIN_ID);
                    }
                }
            }
        }

        // TAGLINE
        if (OverrideTools.checkOverwriteTagline(movie, IMDB_PLUGIN_ID)) {
            movie.setTagline(extractTagline(xml), IMDB_PLUGIN_ID);
        }

        // TV SHOW
        if (movie.isTVShow()) {
            updateTVShowInfo(movie);
        }
    }

    /**
     * Look for the tagline in the XML
     *
     * @param xml The source XML
     * @return The tagline found (or UNKNOWN)
     */
    private static String extractTagline(String xml) {
        String tagline = UNKNOWN;

        // Look for the tagline with upto 3 characters after the sitedef to ensure we get any plurals on the end
        Pattern pTagline = Pattern.compile("(Tagline.{0,3}?:</h\\d>)", Pattern.CASE_INSENSITIVE);
        Matcher m = pTagline.matcher(xml);

        if (m.find()) {
            int beginIndex = m.start();
            // We need to work out which of the two formats to use, this is dependent on which comes first "<span" or "</div"
            String endMarker;
            if (StringUtils.indexOf(xml, "<span", beginIndex) < StringUtils.indexOf(xml, HTML_DIV_END, beginIndex)) {
                endMarker = "<span";
            } else {
                endMarker = HTML_DIV_END;
            }

            // Now look for the right string
            tagline = HTMLTools.stripTags(HTMLTools.extractTag(xml, m.group(1), endMarker), false);
            tagline = cleanStringEnding(tagline);
        }

        return tagline;
    }

    /**
     * Process the plot from the Combined XML
     *
     * @param movie
     * @param xml
     */
    private void getPlot(Movie movie, String xml) {
        String xmlPlot = UNKNOWN;

        // Get the long plot from the summary page
        if (imdbPlot.equalsIgnoreCase("long")) {
            xmlPlot = getPlotSummary(movie);
        }

        // Search on the combined page
        if (isNotValidString(xmlPlot)) {
            xmlPlot = HTMLTools.extractTag(xml, "<h5>Plot" + HTML_H5_END, HTML_DIV_END);
            xmlPlot = HTMLTools.removeHtmlTags(xmlPlot).trim();

            // This plot didn't work, look for another version
            if (isNotValidString(xmlPlot)) {
                xmlPlot = HTMLTools.extractTag(xml, "<h5>Plot</h5>", "<span class=\"");
                xmlPlot = HTMLTools.removeHtmlTags(xmlPlot).trim();
            }

            // This plot didn't work, look for another version
            if (isNotValidString(xmlPlot)) {
                xmlPlot = HTMLTools.extractTag(xml, "<h5>Plot</h5>", "<p>");
                xmlPlot = HTMLTools.removeHtmlTags(xmlPlot).trim();
            }

            // See if the plot has the "metacritic" text and remove it
            int pos = xmlPlot.indexOf("Metacritic.com)");
            if (pos > 0) {
                xmlPlot = xmlPlot.substring(pos + "Metacritic.com)".length());
            }

            // Check the length of the plot is OK
            if (isValidString(xmlPlot)) {
                xmlPlot = cleanStringEnding(xmlPlot);
            } else {
                // The plot might be blank or null so set it to UNKNOWN
                xmlPlot = UNKNOWN;
            }
        }

        // Update the plot with the found plot, or the outline if not found
        if (isValidString(xmlPlot)) {
            movie.setPlot(xmlPlot, IMDB_PLUGIN_ID);
        } else {
            movie.setPlot(movie.getOutline(), IMDB_PLUGIN_ID);
        }
    }

    /**
     * Retrieves the long plot description from IMDB if it exists, else "UNKNOWN"
     *
     * @param movie
     * @return long plot
     */
    private String getPlotSummary(Identifiable movie) {
        String plot = Movie.UNKNOWN;

        String xml = getImdbData(getImdbUrl(movie, SUFFIX_PLOTSUMMARY));

        String result = HTMLTools.extractTag(xml, "<p class=\"plotSummary\">", "</p>");
        if (isValidString(result) && !result.contains("It looks like we don't have any Synopsis for this title yet.")) {
            plot = HTMLTools.stripTags(result);
        }

        // second parsing other site
        result = HTMLTools.extractTag(xml, "<div id=\"swiki.2.1\">", HTML_DIV_END);
        if (isValidString(result) && !result.contains("It looks like we don't have any Synopsis for this title yet.")) {
            plot = HTMLTools.stripTags(result);
        }

        return plot;
    }

    /**
     * Scrape info which is common for old and new IMDb.
     *
     * @param movie
     * @param xml
     * @param imdbNewVersion
     */
    private void updateInfoCommon(Movie movie, String xml) {
        // Store the release info page for release info & AKAs
        String releaseInfoXML = UNKNOWN;
        // Store the aka list
        Map<String, String> akas = null;

        // ASPECT RATIO
        if (OverrideTools.checkOverwriteAspectRatio(movie, IMDB_PLUGIN_ID)) {
            // determine start and end string
            String startString;
            String endString;
            if (fullInfo) {
                startString = HTML_H5_START + "Aspect Ratio" + HTML_H5_END + "<div class=\"info-content\">";
                endString = "<a class";
            } else {
                startString = "<h4 class=\"inline\">Aspect Ratio" + HTML_H4_END;
                endString = HTML_DIV_END;
            }

            // find unclean aspect ratio
            String uncleanAspectRatio = HTMLTools.extractTag(xml, startString, endString).trim();

            if (StringTools.isValidString(uncleanAspectRatio)) {
                // remove spaces and replace , with .
                uncleanAspectRatio = uncleanAspectRatio.replace(" ", "").replace(",", ".");
                // set aspect ratio
                movie.setAspectRatio(aspectTools.cleanAspectRatio(uncleanAspectRatio), IMDB_PLUGIN_ID);
            }
        }

        // RELEASE DATE
        if (OverrideTools.checkOverwriteReleaseDate(movie, IMDB_PLUGIN_ID)) {
            // Load the release page from IMDB
            if (StringTools.isNotValidString(releaseInfoXML)) {
                releaseInfoXML = getImdbData(getImdbUrl(movie, SUFFIX_RELEASEINFO));
            }

            Pattern pRelease = Pattern.compile("(?:.*?)\\Q" + preferredCountry + "\\E(?:.*?)\\Qrelease_date\">\\E(.*?)(?:<.*?>)(.*?)(?:</a>.*)");
            Matcher mRelease = pRelease.matcher(releaseInfoXML);

            if (mRelease.find()) {
                String releaseDate = mRelease.group(1) + " " + mRelease.group(2);
                movie.setReleaseDate(releaseDate, IMDB_PLUGIN_ID);
            }
        }

        // ORIGINAL TITLE / AKAS
        if (OverrideTools.checkOverwriteOriginalTitle(movie, IMDB_PLUGIN_ID)) {
            // Load the AKA page from IMDb
            if (StringTools.isNotValidString(releaseInfoXML)) {
                releaseInfoXML = getImdbData(getImdbUrl(movie, SUFFIX_RELEASEINFO));
            }

            // The AKAs are stored in the format "title", "country"
            // therefore we need to look for the preferredCountry and then work backwards
            List<String> akaList = HTMLTools.extractTags(releaseInfoXML, "<a id=\"akas\" name=\"akas\">", HTML_TABLE_END, "<td>", HTML_TD_END, Boolean.FALSE);
            akas = buildAkaMap(akaList);

            String foundValue = null;
            for (Map.Entry<String, String> aka : akas.entrySet()) {
                if (aka.getKey().contains("original title")) {
                    foundValue = aka.getValue().trim();
                    break;
                }
            }
            movie.setOriginalTitle(foundValue, IMDB_PLUGIN_ID);
        }

        // TITLE for preferred country from AKAS
        if (akaScrapeTitle && OverrideTools.checkOverwriteTitle(movie, IMDB_PLUGIN_ID)) {
            // Load the AKA page from IMDb
            if (StringTools.isNotValidString(releaseInfoXML)) {
                releaseInfoXML = getImdbData(getImdbUrl(movie, SUFFIX_RELEASEINFO));
            }

            // The AKAs are stored in the format "title", "country"
            // therefore we need to look for the preferredCountry and then work backwards
            if (akas == null) {
                // Just extract the AKA section from the page
                List<String> akaList = HTMLTools.extractTags(releaseInfoXML, "<a id=\"akas\" name=\"akas\">", HTML_TABLE_END, "<td>", HTML_TD_END, Boolean.FALSE);
                akas = buildAkaMap(akaList);
            }

            String foundValue = null;
            // NOTE: First matching country is the preferred country
            for (String matchCountry : akaMatchingCountries) {

                if (StringUtils.isBlank(matchCountry)) {
                    // must be a valid country setting
                    continue;
                }

                for (Map.Entry<String, String> aka : akas.entrySet()) {
                    int startIndex = aka.getKey().indexOf(matchCountry);
                    if (startIndex > -1) {
                        String extracted = aka.getKey().substring(startIndex);
                        int endIndex = extracted.indexOf('/');
                        if (endIndex > -1) {
                            extracted = extracted.substring(0, endIndex);
                        }

                        boolean valid = Boolean.TRUE;
                        for (String ignore : akaIgnoreVersions) {
                            if (StringUtils.isNotBlank(ignore) && StringUtils.containsIgnoreCase(extracted, ignore.trim())) {
                                valid = Boolean.FALSE;
                                break;
                            }
                        }

                        if (valid) {
                            foundValue = aka.getValue().trim();
                            break;
                        }
                    }
                }

                if (foundValue != null) {
                    // we found a title for the country matcher
                    break;
                }
            }
            movie.setTitle(foundValue, IMDB_PLUGIN_ID);
        }

        // holds the full credits page
        String fullcreditsXML = UNKNOWN;

        // DIRECTOR(S)
        boolean overrideNormal = OverrideTools.checkOverwriteDirectors(movie, IMDB_PLUGIN_ID);
        boolean overridePeople = OverrideTools.checkOverwritePeopleDirectors(movie, IMDB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            boolean found = Boolean.FALSE;

            // get from combined page (same layout as full credits)
            if (fullInfo) {
                found = extractDirectorsFromFullCredits(movie, xml, overrideNormal, overridePeople);
            }

            // get from full credits
            if (!found) {
                if (isNotValidString(fullcreditsXML)) {
                    fullcreditsXML = getImdbData(getImdbUrl(movie, SUFFIX_FULLCREDITS));
                }
                extractDirectorsFromFullCredits(movie, fullcreditsXML, overrideNormal, overridePeople);
            }
        }

        // WRITER(S)
        overrideNormal = OverrideTools.checkOverwriteWriters(movie, IMDB_PLUGIN_ID);
        overridePeople = OverrideTools.checkOverwritePeopleWriters(movie, IMDB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            boolean found = Boolean.FALSE;

            // get from combined page (same layout as full credits)
            if (fullInfo) {
                found = extractWritersFromFullCredits(movie, xml, overrideNormal, overridePeople);
            }

            // get from full credits
            if (!found) {
                if (isNotValidString(fullcreditsXML)) {
                    fullcreditsXML = getImdbData(getImdbUrl(movie, SUFFIX_FULLCREDITS));
                }
                extractWritersFromFullCredits(movie, fullcreditsXML, overrideNormal, overridePeople);
            }
        }

        // CAST
        overrideNormal = OverrideTools.checkOverwriteActors(movie, IMDB_PLUGIN_ID);
        overridePeople = OverrideTools.checkOverwritePeopleActors(movie, IMDB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            boolean found = Boolean.FALSE;

            // get from combined page (same layout as full credits)
            if (fullInfo) {
                found = extractCastFromFullCredits(movie, xml, overrideNormal, overridePeople);
            }

            // get from full credits
            if (!found) {
                if (isNotValidString(fullcreditsXML)) {
                    fullcreditsXML = getImdbData(getImdbUrl(movie, SUFFIX_FULLCREDITS));
                }
                extractCastFromFullCredits(movie, fullcreditsXML, overrideNormal, overridePeople);
            }
        }
    }

    private boolean extractCastFromFullCredits(Movie movie, String fullcreditsXML, boolean overrideNormal, boolean overridePeople) {
        // count for already set cast
        int count = 0;
        // flag to indicate if cast must be cleared
        boolean clearCast = Boolean.TRUE;
        boolean clearPeopleCast = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String actorBlock : HTMLTools.extractTags(fullcreditsXML, "<table class=\"cast_list\">", HTML_TABLE_END, "<td class=\"primary_photo\"", "</tr>")) {
            // skip faceless persons ("loadlate hidden" is present for actors with photos)
            if (skipFaceless && !actorBlock.contains("loadlate hidden")) {
                continue;
            }

            int nmPosition = actorBlock.indexOf("/nm");
            String personID = actorBlock.substring(nmPosition + 1, actorBlock.indexOf("/", nmPosition + 1));

            String name = HTMLTools.stripTags(HTMLTools.extractTag(actorBlock, "itemprop=\"name\">", HTML_SPAN_END));
            String character = HTMLTools.stripTags(HTMLTools.extractTag(actorBlock, "<td class=\"character\">", HTML_TD_END));

            if (overrideNormal) {
                // clear cast if not already done
                if (clearCast) {
                    movie.clearCast();
                    clearCast = Boolean.FALSE;
                }
                // add actor
                movie.addActor(name, IMDB_PLUGIN_ID);
            }

            if (overridePeople) {
                // clear cast if not already done
                if (clearPeopleCast) {
                    movie.clearPeopleCast();
                    clearPeopleCast = Boolean.FALSE;
                }
                // add actor
                movie.addActor(IMDB_PLUGIN_ID + ":" + personID, name, character, imdbInfo.getImdbSite() + IMDB_NAME + personID + "/", UNKNOWN, IMDB_PLUGIN_ID);
            }

            found = Boolean.TRUE;
            count++;
            if (count == actorMax) {
                break;
            }
        }

        return found;
    }

    private boolean extractDirectorsFromFullCredits(Movie movie, String fullcreditsXML, boolean overrideNormal, boolean overridePeople) {
        // count for already set directors
        int count = 0;
        // flag to indicate if directors must be cleared
        boolean clearDirectors = Boolean.TRUE;
        boolean clearPeopleDirectors = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String directorMatch : new String[]{"Directed by","Director","Directors"}) {
            if (fullcreditsXML.contains(HTML_GT + directorMatch + "&nbsp;</h4>")) {
                for (String member : HTMLTools.extractTags(fullcreditsXML, HTML_GT + directorMatch + "&nbsp;</h4>", HTML_TABLE_END, HTML_A_START, HTML_A_END, Boolean.FALSE)) {
                    int beginIndex = member.indexOf("href=\"/name/");
                    if (beginIndex > -1) {
                        String personID = member.substring(beginIndex + 12, member.indexOf("/", beginIndex + 12));
                        String director = member.substring(member.indexOf(HTML_GT, beginIndex) + 1).trim();
                        if (overrideNormal) {
                            // clear directors if not already done
                            if (clearDirectors) {
                                movie.clearDirectors();
                                clearDirectors = Boolean.FALSE;
                            }
                            // add director
                            movie.addDirector(director, IMDB_PLUGIN_ID);
                        }

                        if (overridePeople) {
                            // clear directors if not already done
                            if (clearPeopleDirectors) {
                                movie.clearPeopleDirectors();
                                clearPeopleDirectors = Boolean.FALSE;
                            }
                            // add director, but check that there are no invalid characters in the name which may indicate a bad scrape
                            if (StringUtils.containsNone(director, "<>:/")) {
                                movie.addDirector(IMDB_PLUGIN_ID + ":" + personID, director, imdbInfo.getImdbSite() + IMDB_NAME + personID + "/", IMDB_PLUGIN_ID);
                                found = Boolean.TRUE;
                                count++;
                            } else {
                                LOG.debug("Invalid director name found: '{}'", director);
                            }
                        }

                        if (count == directorMax) {
                            break;
                        }
                    }
                }
            }
            if (found) {
                // We found a match, so stop search.
                break;
            }
        }

        return found;
    }

    private boolean extractWritersFromFullCredits(Movie movie, String fullcreditsXML, boolean overrideNormal, boolean overridePeople) {
        // count for already set writers
        int count = 0;
        // flag to indicate if writers must be cleared
        boolean clearWriters = Boolean.TRUE;
        boolean clearPeopleWriters = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String writerMatch : new String[]{"Writing credits","Writer","Writers"}) {
            if (StringUtils.indexOfIgnoreCase(fullcreditsXML, HTML_GT + writerMatch) >= 0) {
                for (String member : HTMLTools.extractTags(fullcreditsXML, HTML_GT + writerMatch, HTML_TABLE_END, HTML_A_START, HTML_A_END, Boolean.FALSE)) {
                    int beginIndex = member.indexOf("href=\"/name/");
                    if (beginIndex > -1) {
                        String personID = member.substring(beginIndex + 12, member.indexOf("/", beginIndex + 12));
                        String name = StringUtils.trimToEmpty(member.substring(member.indexOf(HTML_GT, beginIndex) + 1));
                        if (!name.contains("more credit")) {

                            if (overrideNormal) {
                                // clear writers if not already done
                                if (clearWriters) {
                                    movie.clearWriters();
                                    clearWriters = Boolean.FALSE;
                                }
                                // add writer
                                movie.addWriter(name, IMDB_PLUGIN_ID);
                            }

                            if (overridePeople) {
                                // clear writers if not already done
                                if (clearPeopleWriters) {
                                    movie.clearPeopleWriters();
                                    clearPeopleWriters = Boolean.FALSE;
                                }
                                // add writer
                                movie.addWriter(IMDB_PLUGIN_ID + ":" + personID, name, imdbInfo.getImdbSite() + IMDB_NAME + personID + "/", IMDB_PLUGIN_ID);
                            }

                            found = Boolean.TRUE;
                            count++;
                            if (count == writerMax) {
                                break;
                            }
                        }
                    }
                }
            }

            if (found) {
                // We found a match, so stop search.
                break;
            }
        }

        return found;
    }

    /**
     * Process a awards in the IMDb web page
     *
     * @param movie
     * @return
     */
    private boolean updateAwards(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);

        String awardXML = getImdbData(getImdbUrl(imdbId, IMDB_TITLE, SUFFIX_AWARDS));
        if (awardXML.contains("<h1 class=\"header\">Awards</h1>")) {

            List<String> awardHtmlList = HTMLTools.extractTags(awardXML, "<h1 class=\"header\">Awards</h1>", "<div class=\"article\"", "<h3>", "</table>", false);

            Collection<AwardEvent> awardList = new ArrayList<>();
            for (String awardBlock : awardHtmlList) {
                String awardEvent = awardBlock.substring(0, awardBlock.indexOf('<')).trim();

                AwardEvent aEvent = new AwardEvent();
                aEvent.setName(awardEvent);

                String tmpString = HTMLTools.extractTag(awardBlock, "<a href=", HTML_A_END).trim();
                tmpString = tmpString.substring(tmpString.indexOf('>') + 1).trim();
                int awardYear = NumberUtils.isNumber(tmpString) ? Integer.parseInt(tmpString) : -1;

                tmpString = StringUtils.trimToEmpty(HTMLTools.extractTag(awardBlock, "<span class=\"award_category\">", "</span>"));
                Award aAward = new Award();
                aAward.setName(tmpString);
                aAward.setYear(awardYear);

                boolean awardOutcomeWon = true;
                for (String outcomeBlock : HTMLTools.extractHtmlTags(awardBlock, "<table class=", null, "<tr>", "</tr>")) {
                    String outcome = HTMLTools.extractTag(outcomeBlock, "<b>", "</b>");
                    if (StringTools.isValidString(outcome)) {
                        awardOutcomeWon = outcome.equalsIgnoreCase("won");
                    }

                    String awardDescription = StringUtils.trimToEmpty(HTMLTools.extractTag(outcomeBlock, "<td class=\"award_description\">", "<br />"));
                    // Check to see if there was a missing title and just the name in the result
                    if (awardDescription.contains("href=\"/name/")) {
                        awardDescription = StringUtils.trimToEmpty(HTMLTools.extractTag(outcomeBlock, "<span class=\"award_category\">", "</span>"));
                    }

                    if (awardOutcomeWon) {
                        aAward.addWon(awardDescription);
                    } else {
                        aAward.addNomination(awardDescription);
                    }
                }

                if (!scrapeWonAwards || (aAward.getWon() > 0)) {
                    LOG.debug("{} - Adding award: {}", movie.getBaseName(), aAward.toString());
                    aEvent.addAward(aAward);
                }

                if (!aEvent.getAwards().isEmpty()) {
                    awardList.add(aEvent);
                }
            }

            if (!awardList.isEmpty()) {
                movie.setAwards(awardList);
            }
        } else {
            LOG.debug("No awards found for {}", movie.getBaseName());
        }
        return Boolean.TRUE;
    }

    /**
     * Process financial information in the IMDb web page
     *
     * @param movie
     * @return
     */
    private boolean updateBusiness(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        String xml = getImdbData(getImdbUrl(imdbId, IMDB_TITLE, SUFFIX_BUSINESS));

        if (isValidString(xml)) {
            String budget = HTMLTools.extractTag(xml, "<h5>Budget</h5>", HTML_BREAK).replaceAll("\\s.*", "");
            movie.setBudget(budget);
            NumberFormat moneyFormat = NumberFormat.getNumberInstance(new Locale("US"));
            for (int i = 0; i < 2; i++) {
                for (String oWeek : HTMLTools.extractTags(xml, HTML_H5_START + (i == 0 ? "Opening Weekend" : "Gross") + "</h5", HTML_H5_START, "", "<br/")) {
                    if (isValidString(oWeek)) {
                        String currency = oWeek.replaceAll("\\d+.*", "");
                        long value = NumberUtils.toLong(oWeek.replaceAll("^\\D*\\s*", "").replaceAll("\\s.*", "").replaceAll(",", ""), -1L);
                        String country = HTMLTools.extractTag(oWeek, "(", ")");
                        if (country.equals("Worldwide") && !currency.equals("$")) {
                            continue;
                        }
                        String money = i == 0 ? movie.getOpenWeek(country) : movie.getGross(country);
                        if (isValidString(money)) {
                            long m = NumberUtils.toLong(money.replaceAll("^\\D*\\s*", "").replaceAll(",", ""), -1L);
                            value = i == 0 ? value + m : value > m ? value : m;
                        }
                        if (i == 0) {
                            movie.setOpenWeek(country, currency + moneyFormat.format(value));
                        } else {
                            movie.setGross(country, currency + moneyFormat.format(value));
                        }
                    }
                }
            }
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Process trivia in the IMDb web page
     *
     * @param movie
     * @return
     * @throws IOException
     */
    private boolean updateTrivia(Movie movie) {
        if (triviaMax == 0) {
            return Boolean.FALSE;
        }

        String xml = getImdbData(getImdbUrl(movie, SUFFIX_TRIVIA));

        if (isValidString(xml)) {
            int i = 0;
            for (String tmp : HTMLTools.extractTags(xml, "<div class=\"list\">", "<div class=\"list\">", "<div class=\"sodatext\"", HTML_DIV_END)) {
                if (i < triviaMax || triviaMax == -1) {
                    tmp = HTMLTools.removeHtmlTags(tmp);
                    tmp = tmp.trim();
                    movie.addDidYouKnow(tmp);
                    i++;
                } else {
                    break;
                }
            }
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Parse the rating
     *
     * @param rating
     * @return
     */
    private static int parseRating(String rating) {
        StringTokenizer st = new StringTokenizer(rating, "/ ()");
        return StringTools.parseRating(st.nextToken());
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

        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || isNotValidString(imdbId)) {
            return;
        }

        String xml = getImdbData(getImdbUrl(movie, "/episodes?season=" + movie.getSeason()));

        if (StringUtils.isBlank(xml)) {
            return;
        }

        for (MovieFile file : movie.getMovieFiles()) {

            for (int episode = file.getFirstPart(); episode <= file.getLastPart(); ++episode) {

                int beginIndex = xml.indexOf("<meta itemprop=\"episodeNumber\" content=\"" + episode + "\"/>");
                if (beginIndex == -1) {
                    continue;
                }
                int endIndex = xml.indexOf("<div class=\"clear\"", beginIndex);
                String episodeXml = xml.substring(beginIndex, endIndex);

                if (OverrideTools.checkOverwriteEpisodeTitle(file, episode, IMDB_PLUGIN_ID)) {
                    String episodeName = HTMLTools.extractTag(episodeXml, "itemprop=\"name\">", HTML_A_END);
                    file.setTitle(episode, episodeName, IMDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteEpisodePlot(file, episode, IMDB_PLUGIN_ID)) {
                    String plot = HTMLTools.extractTag(episodeXml, "itemprop=\"description\">", HTML_DIV_END);
                    file.setPlot(episode, plot, IMDB_PLUGIN_ID);
                }

                if (OverrideTools.checkOverwriteEpisodeFirstAired(file, episode, IMDB_PLUGIN_ID)) {
                    String firstAired = HTMLTools.extractTag(episodeXml, "<div class=\"airdate\">", "</div>");
                    file.setFirstAired(episode, firstAired, IMDB_PLUGIN_ID);
                }
            }
        }
    }

    /**
     * Get the TV show information from IMDb
     *
     * @param movie
     *
     * @throws IOException
     */
    protected void updateTVShowInfo(Movie movie) {
        scanTVShowTitles(movie);
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        boolean result = Boolean.TRUE;

        // If we already have the ID, skip the scanning of the NFO file
        if (StringTools.isValidString(movie.getId(IMDB_PLUGIN_ID))) {
            return result;
        }

        LOG.debug("Scanning NFO for Imdb Id");
        String id = searchIMDB(nfo, movie);
        if (isValidString(id)) {
            movie.setId(IMDB_PLUGIN_ID, id);
            LOG.debug("IMDb Id found in nfo: {}", movie.getId(IMDB_PLUGIN_ID));
        } else {
            int beginIndex = nfo.indexOf("/tt");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©\"'(--ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¨_ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â )=$");
                movie.setId(IMDB_PLUGIN_ID, st.nextToken());
                LOG.debug("IMDb Id found in nfo: {}", movie.getId(IMDB_PLUGIN_ID));
            } else {
                beginIndex = nfo.indexOf("/Title?");
                if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                    StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©\"'(--ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¨_ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â§ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â )=$");
                    movie.setId(IMDB_PLUGIN_ID, "tt" + st.nextToken());
                    LOG.debug("IMDb Id found in nfo: {}", movie.getId(IMDB_PLUGIN_ID));
                } else {
                    LOG.debug("No IMDb Id found in nfo !");
                    result = Boolean.FALSE;
                }
            }
        }
        return result;
    }

    /**
     * Search for the IMDB Id in the NFO file
     *
     * @param nfo
     * @param movie
     * @return
     */
    private static String searchIMDB(String nfo, Movie movie) {
        final int flags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
        String imdbPattern = ")[\\W].*?(tt\\d{7})";
        // Issue 1912 escape special regex characters in title
        String title = Pattern.quote(movie.getTitle());
        String id = UNKNOWN;

        Pattern patternTitle;
        Matcher matchTitle;

        try {
            patternTitle = Pattern.compile("(" + title + imdbPattern, flags);
            matchTitle = patternTitle.matcher(nfo);
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
        } catch (Exception error) {
            LOG.error("Error locating the IMDb ID in the nfo file for {}", movie.getBaseFilename());
            LOG.error(error.getMessage());
        }

        return StringUtils.trim(id);
    }

    /**
     * Remove the "see more" or "more" values from the end of a string
     *
     * @param uncleanString
     * @return
     */
    protected static String cleanStringEnding(String uncleanString) {
        int pos = uncleanString.indexOf("more");
        // First let's check if "more" exists in the string
        if (pos > 0) {
            if (uncleanString.endsWith("more")) {
                return uncleanString.substring(0, uncleanString.length() - 4).trim();
            }

            pos = uncleanString.toLowerCase().indexOf("see more");
            if (pos > 0) {
                return uncleanString.substring(0, pos).trim();
            }
        }

        pos = uncleanString.toLowerCase().indexOf("full summary");
        if (pos > 0) {
            return uncleanString.substring(0, pos).trim();
        }

        return uncleanString.trim();
    }

    @Override
    public boolean scan(Person person) {
        String imdbId = person.getId(IMDB_PLUGIN_ID);
        if (isNotValidString(imdbId)) {
            LOG.debug("Looking for IMDB ID for {}", person.getName());
            String movieId = UNKNOWN;
            for (Movie movie : person.getMovies()) {
                movieId = movie.getId(IMDB_PLUGIN_ID);
                if (isValidString(movieId)) {
                    break;
                }
            }
            imdbId = imdbInfo.getImdbPersonId(person.getName(), movieId);
            person.setId(IMDB_PLUGIN_ID, imdbId);
        }

        boolean retval = Boolean.TRUE;
        if (isValidString(imdbId)) {
            retval = updateImdbPersonInfo(person);
        } else {
            LOG.debug("IMDB ID not found for {}", person.getName());
        }
        return retval;
    }

    /**
     * Scan IMDB HTML page for the specified person
     */
    private boolean updateImdbPersonInfo(Person person) {
        String imdbID = person.getId(IMDB_PLUGIN_ID);
        if (!imdbID.startsWith("nm")) {
            imdbID = "nm" + imdbID;
            // Correct the ID if it's wrong
            person.setId(IMDB_PLUGIN_ID, "nm" + imdbID);
        }

        LOG.info("Getting information for {} ({})", person.getName(), imdbID);
        String xml = getImdbData(getImdbUrl(person));

        // We can work out if this is the new site by looking for " - IMDb" at the end of the title
        String title = HTMLTools.extractTag(xml, "<title>");
        // Check for the new version and correct the title if found.
        if (title.toLowerCase().endsWith(" - imdb")) {
            title = title.substring(0, title.length() - 7);
        }
        if (title.toLowerCase().startsWith("imdb - ")) {
            title = title.substring(7);
        }
        person.setName(title);

        return updateInfo(person, xml);
    }

    /**
     * Process the new IMDb format web page
     *
     * @param person
     * @param xml
     * @return
     */
    private boolean updateInfo(Person person, String xml) {
        person.setUrl(ImdbPlugin.this.getImdbUrl(person));

        if (xml.contains("Alternate Names:")) {
            String name = HTMLTools.extractTag(xml, "Alternate Names:</h4>", HTML_DIV_END);
            if (isValidString(name)) {
                for (String item : name.split("<span>\\|</span>")) {
                    person.addAka(StringUtils.trimToEmpty(item));
                }
            }
        }

        if (xml.contains("id=\"img_primary\"")) {
            LOG.debug("Looking for image on webpage for {}", person.getName());
            String photoURL = HTMLTools.extractTag(xml, "id=\"img_primary\"", HTML_TD_END);

            if (photoURL.contains("http://ia.media-imdb.com/images")) {
                photoURL = "http://ia.media-imdb.com/images" + HTMLTools.extractTag(photoURL, "src=\"http://ia.media-imdb.com/images", "\"");
                if (isValidString(photoURL)) {
                    person.setPhotoURL(photoURL);
                    person.setPhotoFilename();
                }
            }
        } else {
            LOG.debug("No image found on webpage for {}", person.getName());
        }

        // get personal information
        String xmlInfo = getImdbData(getImdbUrl(person, SUFFIX_BIO));

        StringBuilder date = new StringBuilder();
        int endIndex;
        int beginIndex = xmlInfo.indexOf(">Date of Birth</td>");

        if (beginIndex > -1) {
            endIndex = xmlInfo.indexOf(">Date of Death</td>");
            beginIndex = xmlInfo.indexOf("birth_monthday=", beginIndex);
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                Matcher m = PATTERN_DOB.matcher(xmlInfo.substring(beginIndex + 15, beginIndex + 20));
                if (m.find()) {
                    date.append(m.group(2)).append("-").append(m.group(1));
                }
            }

            beginIndex = xmlInfo.indexOf("birth_year=", beginIndex);
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                if (date.length() > 0) {
                    date.append("-");
                }
                date.append(xmlInfo.substring(beginIndex + 11, beginIndex + 15));
            }

            beginIndex = xmlInfo.indexOf("birth_place=", beginIndex);
            String place;
            if (beginIndex > -1) {
                place = HTMLTools.extractTag(xmlInfo, "birth_place=", HTML_A_END);
                int start = place.indexOf('>');
                if (start > -1 && start < place.length()) {
                    place = place.substring(start + 1);
                }
                if (isValidString(place)) {
                    person.setBirthPlace(place);
                }
            }
        }

        beginIndex = xmlInfo.indexOf(">Date of Death</td>");
        if (beginIndex > -1) {
            endIndex = xmlInfo.indexOf(">Mini Bio (1)</h4>", beginIndex);
            beginIndex = xmlInfo.indexOf("death_monthday=", beginIndex);
            StringBuilder dDate = new StringBuilder();
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                Matcher m = PATTERN_DOB.matcher(xmlInfo.substring(beginIndex + 15, beginIndex + 20));
                if (m.find()) {
                    dDate.append(m.group(2));
                    dDate.append("-");
                    dDate.append(m.group(1));
                }
            }
            beginIndex = xmlInfo.indexOf("death_date=", beginIndex);
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                if (dDate.length() > 0) {
                    dDate.append("-");
                }
                dDate.append(xmlInfo.substring(beginIndex + 11, beginIndex + 15));
            }
            if (dDate.length() > 0) {
                date.append("/").append(dDate);
            }
        }

        if (StringUtils.isNotBlank(date)) {
            person.setYear(date.toString());
        }

        beginIndex = xmlInfo.indexOf(">Birth Name</td>");
        if (beginIndex > -1) {
            beginIndex += 20;
            String name = xmlInfo.substring(beginIndex, xmlInfo.indexOf(HTML_TD_END, beginIndex));
            if (isValidString(name)) {
                person.setBirthName(HTMLTools.decodeHtml(name));
            }
        }

        beginIndex = xmlInfo.indexOf(">Nickname</td>");
        if (beginIndex > -1) {
            String name = xmlInfo.substring(beginIndex + 17, xmlInfo.indexOf(HTML_TD_END, beginIndex + 17));
            if (isValidString(name)) {
                person.addAka(name);
            }
        } else {
            beginIndex = xmlInfo.indexOf(">Nicknames</td>");
            if (beginIndex > -1) {
                String name = xmlInfo.substring(beginIndex + 19, xmlInfo.indexOf(HTML_TD_END, beginIndex + 19));
                for (String n : name.split("<br>")) {
                    person.addAka(n.trim());
                }
            }
        }

        Matcher m = PATTERN_BIO.matcher(xmlInfo);
        if (m.find()) {
            String bio = HTMLTools.stripTags(m.group(1), true);
            if (isValidString(bio)) {
                bio = trimToLength(bio, preferredBiographyLength);
                person.setBiography(bio);
            }
        }

        // get known movies
        xmlInfo = getImdbData(getImdbUrl(person, SUFFIX_FILMOYEAR));
        if (xmlInfo.contains("<div id=\"tn15content\">")) {
            int count = HTMLTools.extractTags(xmlInfo, "<div id=\"tn15content\">", HTML_DIV_END, "<li>", "</li>").size();
            person.setKnownMovies(count);
        }

        // get filmography
        processFilmography(person, xml);

        int version = person.getVersion();
        person.setVersion(++version);
        return Boolean.TRUE;
    }

    /**
     * Process the person's filmography from the source XML
     *
     * @param person
     * @param sourceXml
     */
    protected void processFilmography(Person person, String sourceXml) {
        int beginIndex, endIndex;

        if (!sourceXml.contains("<h2>Filmography</h2>")) {
            LOG.info("No filmography found for {}", person.getName());
            return;
        }

        // List of films for the person
        Map<String, Filmography> filmography = new TreeMap<>();

        Matcher mJobList = pJobSection.matcher(sourceXml);

        // Loop around the jobs
        while (mJobList.find()) {
            // The current job type we are processing
            String currentJob = mJobList.group(1);
            // Save the start of the section
            beginIndex = mJobList.start();
            // Find the end of the section
            endIndex = sourceXml.indexOf("<div id=\"filmo-", beginIndex);
            if (endIndex < 0) {
                // This might be the last section, so search for the end
                endIndex = sourceXml.indexOf("<h2>Related Videos</h2>", beginIndex);
                if (endIndex < 0) {
                    LOG.warn("Failed to locate the end of the job list for {} - '{}'", person.getName(), currentJob);
                    break;
                }
            }

            if (jobsInclude.contains(currentJob)) {
                LOG.trace("Job: '{}' with '{}' credits is required", currentJob, mJobList.group(2));
            } else {
                LOG.trace("Job: '{}' with '{}' credits is NOT required", currentJob, mJobList.group(2));
                // Skip this job
                continue;
            }

            String videoList = sourceXml.substring(beginIndex, endIndex);
            Matcher mJobs = pJobItems.matcher(videoList);

            Matcher mJob;
            int count = 1;
            while (mJobs.find() && count <= preferredFilmographyMax) {
                String jobItem = mJobs.group(1).trim();
                LOG.trace("{} #{}: {}", currentJob, count++, jobItem);

                if (checkSkips(jobItem)) {
                    continue;
                }

                String title;
                String id;

                /*
                 * Generic stuff to all jobs
                 */
                mJob = pJobIdTitle.matcher(jobItem);
                if (mJob.find()) {
                    id = mJob.group(1);
                    title = mJob.group(2);
                    // Strip out anything after the (
                    if (title.contains("(")) {
                        title = title.substring(0, title.indexOf('('));
                    }

                    // Remove any HTML tags
                    title = HTMLTools.stripTags(title, true);
                } else {
                    LOG.warn("No ID and Title found");
                    continue;
                }

                // Create the filmography
                Filmography film = filmography.get(id);
                if (film == null) {
                    film = new Filmography();
                    film.setId(id);
                    film.setName(title);
                    film.setJob(currentJob);
                    film.setUrl(getImdbUrl(id, IMDB_TITLE, null));
                    filmography.put(id, film);
                } else {
                    LOG.debug("Film '{}' already exists for {} as '{}', skipping '{}'",
                            film.getTitle(), person.getName(), film.getJob(), currentJob);
                    continue;
                }

                // YEAR
                mJob = pJobYear.matcher(jobItem);
                if (mJob.find()) {
                    film.setYear(mJob.group(1));
                } else {
                    LOG.debug("No year found for {} in '{}'", person.getName(), title);
                }

                /*
                 * Specific job processing
                 */
                switch (currentJob.toLowerCase()) {
                    case "actor":
                    case "acress":
                        processActorItem(film, jobItem);
                        break;
                    case "producer":
                        film.setJob(Filmography.JOB_PRODUCER);
                        break;
                    case "writer":
                        film.setJob(Filmography.JOB_WRITER);
                        break;
                    case "director":
                        film.setJob(Filmography.JOB_DIRECTOR);
                        break;
                    default:
                        film.setJob(currentJob);
                        break;
                }
                film.setDepartment();
            }
        }

        // Add the information about the film
        updateFilmography(person, filmography);
    }

    /**
     * Check the films that have missing characters
     *
     * @param person Person to check
     * @param filmography The filmography to scan
     */
    private void updateFilmography(Person person, Map<String, Filmography> filmography) {
        int beginIndex, endIndex;

        Iterator<String> iterFilm = filmography.keySet().iterator();
        int count = 0;
        while (iterFilm.hasNext() && count < preferredFilmographyMax) {
            Filmography film = filmography.get(iterFilm.next());

            LOG.trace("Updating '{}' {}: {} - {}", film.getTitle(), film.getDepartment(), film.getJob(), film.getCharacter());
            if (Filmography.DEPT_ACTORS.equals(film.getDepartment()) && isNotValidString(film.getCharacter())) {
                String movieXML = getImdbData(getImdbUrl(film.getId(), IMDB_TITLE, SUFFIX_FULLCREDITS));

                if (StringUtils.isBlank(movieXML)) {
                    continue;
                }

                beginIndex = movieXML.indexOf("(in credits order)");
                if (beginIndex < 0) {
                    // Try an alternative search
                    beginIndex = movieXML.indexOf("name=\"cast\" id=\"cast\"");
                }

                String character = Movie.UNKNOWN;
                if (beginIndex > -1) {
                    endIndex = movieXML.indexOf(">Produced by", beginIndex);
                    endIndex = endIndex < 0 ? movieXML.length() : endIndex;

                    character = HTMLTools.extractTag(movieXML.substring(beginIndex, endIndex), "<a href=\"/name/" + person.getId(), "</tr>");
                    character = HTMLTools.stripTags(HTMLTools.extractTag(character, "<td class=\"character\">", "</td>"));

                    // Remove any text in brackets
                    endIndex = character.indexOf('(');
                    if (endIndex > -1) {
                        character = character.substring(0, endIndex);
                    }
                }

                if (isValidString(character)) {
                    LOG.trace("Found character '{}' for {}", character, person.getName());
                    film.setCharacter(character);
                }
            }
            person.addFilm(film);
            count++;
        }
    }

    /**
     * Check the XML against a set of skip conditions.
     *
     * @param jobItem
     * @return True if the item should be skipped
     */
    private boolean checkSkips(final String jobItem) {
        if (skipTV
                && (jobItem.contains("filmo-episodes") || jobItem.contains("TV Series") || jobItem.contains("Video") || jobItem.contains("TV Special"))) {
            LOG.trace("Skipping because it's a TV Show");
            return true;
        }

        if (skipVG
                && jobItem.contains("(Video Game)")) {
            LOG.trace("Skipping because it's a video game");
            return true;
        }

        if (skipV
                && jobItem.contains("(Video)")) {
            LOG.trace("Skipping because it's a video");
            return true;
        }

        return false;
    }

    /**
     * Find the character from the XML item string
     *
     * @param item
     * @return
     */
    private static String getCharacter(final String item) {
        String character = UNKNOWN;
        int beginIndex, endIndex;

        LOG.trace("Looking for character in '{}'", item);

        String charBegin = "<a href=\"/character/";
        String charEnd = "</a>";

        beginIndex = item.indexOf(charBegin);
        if (beginIndex > -1) {
            endIndex = item.indexOf(charEnd, beginIndex);
            endIndex = endIndex < 0 ? item.length() : endIndex + charEnd.length();

            character = HTMLTools.stripTags(item.substring(beginIndex, endIndex), true);

            // Remove any text in brackets
            endIndex = character.indexOf('(');
            if (endIndex > -1) {
                character = StringUtils.trimToEmpty(character.substring(0, endIndex));
            }
        } else {
            // Try an alternative method to get the character
            // It's usually at the end of the string between <br/> and </div>
            beginIndex = item.lastIndexOf("<br/>");
            endIndex = item.lastIndexOf("</div>");
            if (endIndex > beginIndex) {
                character = HTMLTools.stripTags(item.substring(beginIndex, endIndex), true);
                // Remove anything in ()
                character = character.replaceAll("\\([^\\)]*\\)", "");
            }
        }

        LOG.trace("Returning character: '{}'", StringTools.isValidString(character) ? character : UNKNOWN);
        return StringTools.isValidString(character) ? character : UNKNOWN;
    }

    /**
     * Process actor/actress specific items from the job
     *
     * @param film The Filmography to add the information to
     * @param jobItem The source XML to process
     */
    private static void processActorItem(Filmography film, final String jobItem) {
        film.setCharacter(getCharacter(jobItem));
        film.setJob(Filmography.JOB_ACTOR);
    }

    /**
     * Create a map of the AKA values
     *
     * @param list
     * @return
     */
    private static Map<String, String> buildAkaMap(List<String> list) {
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        do {
            try {
                String key = list.get(i++);
                String value = list.get(i++);
                map.put(key, value);
            } catch (Exception ignore) {
                i = -1;
            }
        } while (i != -1);
        return map;
    }

    private String getImdbData(String url) {
        String data;
        try {
            data = httpClient.request(url, imdbInfo.getCharset());
        } catch (IOException ex) {
            LOG.warn("Failed to get web page ({}) from IMDB: {}", url, ex.getMessage(), ex);
            data = StringUtils.EMPTY;
        }

        return data;
    }

    /**
     * Get the IMDb URL with the default site definition
     *
     * @param item An identifiable object to get the ID from
     * @return
     */
    protected String getImdbUrl(Identifiable item) {
        return this.getImdbUrl(item, null);
    }

    /**
     * Get the IMDb URL with the default site definition
     *
     * @param item An identifiable object to get the ID from
     * @param typeSuffix The suffix, optional
     * @return
     */
    protected String getImdbUrl(Identifiable item, String typeSuffix) {
        String type;
        if (item instanceof Person) {
            type = IMDB_NAME;
        } else {
            type = IMDB_TITLE;
        }
        return getImdbUrl(item.getId(IMDB_PLUGIN_ID), type, typeSuffix);
    }

    /**
     * Get the IMDB URL for the ID
     *
     * @param siteDefinition The current site definition
     * @param id The ID, either person or movie
     * @param type The URL Type to get - Must not start or end with "/"
     * @param typeSuffix The suffix, optional
     * @return
     */
    protected String getImdbUrl(String id, String type, String typeSuffix) {
        StringBuilder url = new StringBuilder();
        url.append(imdbInfo.getImdbSite());

        if (type.startsWith("/")) {
            url.append(type.substring(1));
        } else {
            url.append(type);
        }

        url.append(id);

        if (StringUtils.isBlank(typeSuffix)) {
            url.append('/');
        } else {
            url.append(typeSuffix);
        }

        return url.toString();
    }
}
