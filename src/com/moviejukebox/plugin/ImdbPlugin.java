package com.moviejukebox.plugin;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.PosterScanner;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.tools.XMLHelper;

public class ImdbPlugin implements MovieDatabasePlugin {

    public static String IMDB_PLUGIN_ID = "imdb";
    private static final String THEMOVIEDB_API_KEY = "228c65452d6c1d823bbe69bd6859ebb8";
    protected static Logger logger = Logger.getLogger("moviejukebox");
    protected String preferredSearchEngine;
    protected boolean perfectMatch;
    protected String preferredCountry;
    protected String imdbPlot;
    protected WebBrowser webBrowser;
    protected boolean downloadFanart;
    protected boolean extractCertificationFromMPAA;
    protected static String fanartToken;

    public ImdbPlugin() {
        webBrowser                  = new WebBrowser();
        preferredSearchEngine       = PropertiesUtil.getProperty("imdb.id.search", "imdb");
        perfectMatch                = Boolean.parseBoolean(PropertiesUtil.getProperty("imdb.perfect.match", "true"));
        preferredCountry            = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
        imdbPlot                    = PropertiesUtil.getProperty("imdb.plot", "short");
        downloadFanart              = Boolean.parseBoolean(PropertiesUtil.getProperty("moviedb.fanart.download", "false"));
        fanartToken                 = PropertiesUtil.getProperty("fanart.scanner.fanartToken", ".fanart");
        extractCertificationFromMPAA = Boolean.parseBoolean(PropertiesUtil.getProperty("imdb.getCertificationFromMPAA", "true"));
    }

    @Override
    public boolean scan(Movie mediaFile) {
        String imdbId = mediaFile.getId(IMDB_PLUGIN_ID);
        if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            imdbId = getImdbId(mediaFile.getTitle(), mediaFile.getYear());
            mediaFile.setId(IMDB_PLUGIN_ID, imdbId);
        }

        boolean retval = true;
        if (!imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            retval = updateImdbMediaInfo(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the IMDb matching the specified movie name and year. This routine is base on a IMDb request.
     */
    protected String getImdbId(String movieName, String year) {
        if ("google".equalsIgnoreCase(preferredSearchEngine)) {
            return getImdbIdFromGoogle(movieName, year);
        } else if ("yahoo".equalsIgnoreCase(preferredSearchEngine)) {
            return getImdbIdFromYahoo(movieName, year);
        } else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
            return Movie.UNKNOWN;
        } else {
            return getImdbIdFromImdb(movieName, year);
        }
    }

    /**
     * Retrieve the IMDb Id matching the specified movie name and year. This routine is base on a yahoo request.
     * 
     * @param   movieName   The name of the Movie to search for
     * @param   year        The year of the movie
     * @return              The IMDb Id if it was found
     */
    private String getImdbIdFromYahoo(String movieName, String year) {
        try {
            StringBuffer sb = new StringBuffer("http://fr.search.yahoo.com/search;_ylt=A1f4cfvx9C1I1qQAACVjAQx.?p=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Aimdb.com&fr=yfp-t-501&ei=UTF-8&rd=r1");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("/title/tt");
            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 7), "/\"");
            String imdbId = st.nextToken();

            if (imdbId.startsWith("tt")) {
                return imdbId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving IMDb Id for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This routine is base on a Google request.
     * 
     * @param   movieName   The name of the Movie to search for
     * @param   year        The year of the movie
     * @return              The IMDb Id if it was found
     */
    private String getImdbIdFromGoogle(String movieName, String year) {
        try {
            StringBuffer sb = new StringBuffer("http://www.google.fr/search?hl=fr&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Awww.imdb.com&meta=");

            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("/title/tt");
            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 7), "/\"");
            String imdbId = st.nextToken();

            if (imdbId.startsWith("tt")) {
                return imdbId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving IMDb Id for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * retrieve the IMDb matching the specified movie name and year. This routine is base on a IMDb request.
     */
    private String getImdbIdFromImdb(String movieName, String year) {
        try {
            StringBuffer sb = new StringBuffer("http://www.imdb.com/find?q=");
            sb.append(URLEncoder.encode(movieName, "iso-8859-1"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }
            sb.append(";s=tt;site=aka");
            
            logger.finest("Querying IMDB for " + sb.toString());
            String xml = webBrowser.request(sb.toString());
            
            // Check if this is an exact match (we got a movie page instead of a results list)
            Pattern titleregex = Pattern.compile("<link rel=\"canonical\" href=\"http://www.imdb.com/title/(tt\\d+)/\"");
            Matcher titlematch = titleregex.matcher(xml);
            if (titlematch.find()) {
                logger.finest("Found exact IMDB match for " + movieName + " (" + year + ")");
                return titlematch.group(1);
            }
            
            Pattern imdbregex = Pattern.compile(
                 "\\<a(?:\\s*[^\\>])\\s*"       // start of a-tag, and cruft before the href
                +"href=\"/title/(tt\\d+)"       // the href, grab the id
                +"(?:\\s*[^\\>])*\\>"           // cruft after the href, to the end of the a-tag
                +"([^\\<]+)\\</a\\>"            // grab link text (ie, title), match to the close a-tag
                +"\\s*"
                +"\\((\\d{4})(?:/[^\\)]+)?\\)"  // year, eg (1999) or (1999/II), grab the 4-digit year only
                +"\\s*"
                +"((?:\\(VG\\))?)"              // video game flag (if present)
            );
            // Groups: 1=id, 2=title, 3=year, 4=(VG)
            
            /* This is what the wiki says imdb.perfect.match is supposed to do, but the result doesn't make a lot of
               sense to me. See the discussion for issue 567.
            if (perfectMatch) {
                // Reorder the titles so that the "Exact Matches" section gets scanned first
                int exactStart = xml.indexOf("Titles (Exact Matches)");
                if (-1 != exactStart) {
                    int exactEnd = xml.indexOf("Titles (Partial Matches)");
                    if (-1 == exactEnd) {
                        exactEnd = xml.indexOf("Titles (Approx Matches)");
                    }
                    if (-1 != exactEnd) {
                        xml = xml.substring(exactStart, exactEnd) + xml.substring(0, exactStart) + xml.substring(exactEnd);
                    } else {
                        xml = xml.substring(exactStart) + xml.substring(0, exactStart);
                    }
                }
            }
            */

            Matcher match = imdbregex.matcher(xml);
            while (match.find()) {
                // Find the first title where the year matches (if present) and that isn't a video game
                if (!"(VG)".equals(match.group(4))
                    // Don't worry about matching title/year info -- IMDB took care of that already.
                    //&& (null == year || Movie.UNKNOWN == year || year.equals(match.group(3)))
                    //&& (!perfectMatch || movieName.equalsIgnoreCase(match.group(2)))
                    ) {
                    logger.finest(movieName + ": found IMDB match, "
                        + match.group(2) + " (" + match.group(3) + ") " + match.group(4));
                    return match.group(1);
                } else {
                    logger.finest(movieName + ": rejected IMDB match "
                        + match.group(2) + " (" + match.group(3) + ") " + match.group(4));
                }
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving IMDb Id for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
        }
        
        return Movie.UNKNOWN;
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
        return value;
    }

    /**
     * Scan IMDB html page for the specified movie
     */
    private boolean updateImdbMediaInfo(Movie movie) {
        try {
            String xml = webBrowser.request("http://www.imdb.com/title/" + movie.getId(IMDB_PLUGIN_ID) + "/");

            if (xml.contains("\"tv-extra\"")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                    return false;
                }
            }

            if (!movie.isOverrideTitle()) {
                movie.setTitle(HTMLTools.extractTag(xml, "<title>", 0, "()><"));
                movie.setOriginalTitle(HTMLTools.extractTag(xml, "<title>", 0, "()><"));
            }
            if (movie.getRating() == -1) {
                movie.setRating(parseRating(HTMLTools.extractTag(xml, "<div class=\"meta\">", 1)));
            }

            if (movie.getTop250() == -1) {
                try {
                    movie.setTop250(Integer.parseInt(HTMLTools.extractTag(xml, "Top 250: #")));
                } catch (NumberFormatException e) {
                    movie.setTop250(-1);
                }
            }
            
            if (movie.getDirector().equals(Movie.UNKNOWN)) {
                // Note this is a hack for the change to IMDB for Issue 875
                // TODO: Change the directors into a collection for better processing.
                ArrayList<String> tempDirectors = null;
                tempDirectors = HTMLTools.extractTags(xml, "<h5>Director", "</div>", "<a href=\"/name/", "</a>");
                
                if (movie.getDirector() == null || movie.getDirector().isEmpty() || movie.getDirector().equalsIgnoreCase(Movie.UNKNOWN)) {
                    if (!tempDirectors.isEmpty()) {
                        movie.setDirector(tempDirectors.get(0));
                    }
                }
            }

            if (movie.getReleaseDate().equals(Movie.UNKNOWN)) {
                movie.setReleaseDate(HTMLTools.extractTag(xml, "<h5>Release Date:</h5>"));
            }

            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                movie.setRuntime(getPreferredValue(HTMLTools.extractTags(xml, "<h5>Runtime:</h5>")));
            }

            if (movie.getCountry().equals(Movie.UNKNOWN)) {
                movie.setCountry(HTMLTools.extractTag(xml, "<h5>Country:</h5>", 1));
            }

            if (movie.getCompany().equals(Movie.UNKNOWN)) {
                movie.setCompany(HTMLTools.extractTag(xml, "<h5>Company:</h5>", 1));
            }

            if (movie.getGenres().isEmpty()) {
                for (String genre : HTMLTools.extractTags(xml, "<h5>Genre:</h5>", "</div>", "<a href=\"/Sections/Genres/", "</a>")) {
                    movie.addGenre(Library.getIndexingGenre(genre));
                }
            }

            String imdbOutline = Movie.UNKNOWN;
            int plotBegin = xml.indexOf("<h5>Plot:</h5>");
            if (plotBegin > -1) {
                plotBegin += "<h5>Plot:</h5>".length();
                int plotEnd = xml.indexOf("<a class=\"tn15more", plotBegin);
                if (plotEnd > -1) {
                    String outline = HTMLTools.stripTags(xml.substring(plotBegin, plotEnd));
                    if (outline.length() > 0) {
                        imdbOutline = outline;
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
                    String mpaa = HTMLTools.extractTag(xml, "<h5><a href=\"/mpaa\">MPAA</a>:</h5>");
                    if (!mpaa.equals(Movie.UNKNOWN)) {
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
                if (certification.equals(Movie.UNKNOWN)) {
                    certification = getPreferredValue(HTMLTools.extractTags(xml, "<h5>Certification:</h5>", "</div>", "<a href=\"/List?certificates=", "</a>"));
                }
                if (certification == null || certification.equalsIgnoreCase(Movie.UNKNOWN)) {
                    certification = Movie.NOTRATED;
                }
                movie.setCertification(certification);
            }

            if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setYear(HTMLTools.extractTag(xml, "<a href=\"/Sections/Years/", 1));
                if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                    movie.setYear(HTMLTools.extractTag(xml, "<h5>Original Air Date:</h5>", 2, " "));
                }
            }

            if (movie.getCast().isEmpty()) {
                movie.setCast(HTMLTools.extractTags(xml, "<table class=\"cast\">", "</table>", "<td class=\"nm\"><a href=\"/name/", "</a>"));
            }
            
            if (movie.getWriters().isEmpty()) {
                movie.setWriters(HTMLTools.extractTags(xml, "<h5>Writers ", "</div>", "<a href=\"/name/", "</a>"));
            }

            if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                movie.setPosterURL(locatePosterURL(movie, xml));
            }

            if (movie.isTVShow()) {
                updateTVShowInfo(movie);
            }

            if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
                movie.setFanartURL(getFanartURL(movie));
                if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                }
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving IMDb rating for movie : " + movie.getId(IMDB_PLUGIN_ID));
            e.printStackTrace();
        }
        return true;
    }

    private int parseRating(String rating) {
        StringTokenizer st = new StringTokenizer(rating, "/ ()");
        try {
            return (int)(Float.parseFloat(st.nextToken()) * 10);
        } catch (Exception e) {
            return -1;
        }
    }

    protected String getFanartURL(Movie movie) {
        String url = Movie.UNKNOWN;

        String imdbID = movie.getId(IMDB_PLUGIN_ID);
        if (imdbID != null && !imdbID.equalsIgnoreCase(Movie.UNKNOWN)) {
            XMLEventReader xmlReader = null;
            try {
                xmlReader = XMLHelper.getEventReader("http://api.themoviedb.org/2.0/Movie.imdbLookup?imdb_id=" + imdbID + "&api_key=" + THEMOVIEDB_API_KEY);

                url = parseNextFanartURL(xmlReader);
            } catch (Exception e) {
                logger.severe("Error looking up fanart from TheMovieDB: " + e.getMessage());
            } finally {
                XMLHelper.closeEventReader(xmlReader);
            }
        }

        return url;
    }

    /**
     * Locate the poster URL from online sources
     * @param   movie   Movie bean for the video to locate
     * @param   imdbXML XML page from IMDB to search for a URL
     * @return          The URL of the poster if found.
     */
    protected String locatePosterURL(Movie movie, String imdbXML) {
        return PosterScanner.getPosterURL(movie, imdbXML, IMDB_PLUGIN_ID);
    }
        
    @Override
    public void scanTVShowTitles(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            return;
        }

        try {
            String xml = webBrowser.request("http://www.imdb.com/title/" + imdbId + "/episodes");
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
        } catch (IOException e) {
            logger.severe("Failed retreiving episodes titles for movie : " + movie.getTitle());
            logger.severe("Error : " + e.getMessage());
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
    private String getLongPlot(Movie movie) {
        String plot = Movie.UNKNOWN;

        try {
            String xml = webBrowser.request("http://www.imdb.com/title/" + movie.getId(IMDB_PLUGIN_ID) + "/plotsummary");

            String result = HTMLTools.extractTag(xml, "<p class=\"plotpar\">");
            if (!result.equalsIgnoreCase(Movie.UNKNOWN) && result.indexOf("This plot synopsis is empty") < 0) {
                plot = result;
            }
        } catch (Exception e) {
            plot = Movie.UNKNOWN;
        }

        return plot;
    }

    @Override
    public void scanNFO(String nfo, Movie movie) {
        logger.finest("Scanning NFO for Imdb Id");
        int beginIndex = nfo.indexOf("/tt");
        if (beginIndex != -1) {
            StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&é\"'(--è_çà)=$");
            movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
            logger.finer("Imdb Id found in nfo = " + movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
        } else {
            beginIndex = nfo.indexOf("/Title?");
            if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&é\"'(--è_çà)=$");
                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
            } else {
                logger.finer("No Imdb Id found in nfo !");
            }
        }
    }

    private String parseNextFanartURL(XMLEventReader xmlReader) throws XMLStreamException {
        String url = Movie.UNKNOWN;

        while (xmlReader.hasNext()) {
            XMLEvent event = xmlReader.nextEvent();
            if (event.isStartElement()) {
                String tag = event.toString();
                if (tag.equalsIgnoreCase("<backdrop size=\"original\">") || tag.equalsIgnoreCase("<backdrop size='original'>")) {
                    url = XMLHelper.getCData(xmlReader);
                    break;
                }
            }
        }
        
        if ( url.equals(Movie.UNKNOWN) ) {
            logger.finest("No fanart found");
        } else {
            logger.finest("Fanart URL: " + url);
        }

        return url;
    }
}
