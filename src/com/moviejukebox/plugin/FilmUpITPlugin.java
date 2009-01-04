package com.moviejukebox.plugin;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class FilmUpITPlugin extends ImdbPlugin {

    public static String FILMUPIT_PLUGIN_ID = "filmupit";
    private static int FILMUPIT_PLUGIN_PLOT_LENGTH_LIMIT = 600;

    public FilmUpITPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Italy");
    }

    /**
     * Scan FilmUp.IT html page for the specified movie
     */
    private boolean updateMovieInfo(Movie movie) {
        try {
            String xml = webBrowser.request("http://filmup.leonardo.it/sc_" + movie.getId(FILMUPIT_PLUGIN_ID) + ".htm");

            if (!movie.isOverrideTitle()) {
                movie.setTitle(extractTag(xml, "<font face=\"arial, helvetica\" size=\"3\"><b>", "</b>"));
            }
            // limit plot to FILMUPIT_PLUGIN_PLOT_LENGTH_LIMIT char
            String tmpPlot = removeHtmlTags(extractTag(xml, "Trama:<br>", "</font><br>"));
            if (tmpPlot.length() > FILMUPIT_PLUGIN_PLOT_LENGTH_LIMIT) {
                tmpPlot = tmpPlot.substring(0, Math.min(tmpPlot.length(), FILMUPIT_PLUGIN_PLOT_LENGTH_LIMIT)) + "...";
            }
            movie.setPlot(tmpPlot);

            movie.setDirector(removeHtmlTags(extractTag(xml, "Regia:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                "</font></td></tr>")));
            movie.setReleaseDate(extractTag(xml, "Data di uscita:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                "</font></td></tr>"));
            movie.setRuntime(extractTag(xml, "Durata:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>"));
            movie.setCountry(extractTag(xml, "Nazione:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>"));
            movie.setCompany(removeHtmlTags(extractTag(xml, "Distribuzione:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                "</font></td></tr>")));

            int count = 0;
            for (String tmp_genre : extractTag(xml, "Genere:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">",
                "</font></td></tr>").split(",")) {
                for (String genre : tmp_genre.split("/")) {
                    movie.addGenre(genre.trim());
                }
            }

            if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase("Unknown")) {
                movie.setYear(extractTag(xml, "Anno:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>"));
            }

            for (String actor : removeHtmlTags(
                extractTag(xml, "Cast:&nbsp;</font></td><td valign=\"top\"><font face=\"arial, helvetica\" size=\"2\">", "</font></td></tr>")).split(",")) {
                movie.addActor(actor.trim());
            }

            String posterPageUrl = extractTag(xml, "href=\"posters/locp/", "\"");
            if (!posterPageUrl.equalsIgnoreCase("Unknown")) {
                updatePoster(movie, posterPageUrl);
            }

            String opinionsPageID = extractTag(xml, "/opinioni/op.php?uid=", "\"");
            if (!opinionsPageID.equalsIgnoreCase("Unknown")) {
                int PageID = Integer.parseInt(opinionsPageID);
                updateRate(movie, PageID);
                logger.finest("Opinions page UID = " + PageID);
            }
        } catch (IOException e) {
            logger.severe("Failed retreiving FilmUP infos for movie : " + movie.getId(FILMUPIT_PLUGIN_ID));
            e.printStackTrace();
        }
        return true;
    }

    private void updateRate(Movie movie, int opinionsPageID) {
        String baseUrl = "http://filmup.leonardo.it/opinioni/op.php?uid=";
        try {
            String xml = webBrowser.request(baseUrl + opinionsPageID);
            float rating = Float.parseFloat(extractTag(xml, "Media Voto:&nbsp;&nbsp;&nbsp;</td><td align=\"left\"><b>", "</b>")) * 10;
            movie.setRating((int)rating);
        } catch (IOException e) {
            logger.severe("Failed retreiving rating for : " + movie.getId(FILMUPIT_PLUGIN_ID));
            e.printStackTrace();
        }
    }

    private void updatePoster(Movie movie, String pageUrl) {
        // make an Imdb request for poster
        if (movie.getPosterURL() != null && !movie.getPosterURL().equalsIgnoreCase("Unknown")) {
            // we already have a poster URL
            logger.finer("Movie already has PosterURL : " + movie.getPosterURL());
            return;
        }
        try {
            String posterURL = "Unknown";
            String xml;
            // Check FilmUp.IT first only for movies because TV Show posters are
            // wrong.
            if (!movie.isTVShow()) {
                String baseUrl = "http://filmup.leonardo.it/posters/locp/";
                xml = webBrowser.request(baseUrl + pageUrl);
                String tmpPosterURL = extractTag(xml, "\"../loc/", "\"");
                if (!tmpPosterURL.equalsIgnoreCase("Unknown")) {
                    posterURL = "http://filmup.leonardo.it/posters/loc/" + tmpPosterURL;
                    logger.finest("Movie PosterURL : " + posterURL);
                    movie.setPosterURL(posterURL);
                    return;
                }
            }
            // Check posters.motechnet.com
            if (this.testMotechnetPoster(movie.getId(FILMUPIT_PLUGIN_ID))) {
                posterURL = "http://posters.motechnet.com/covers/" + movie.getId(FILMUPIT_PLUGIN_ID) + "_largeCover.jpg";
                logger.finest("Movie PosterURL : " + posterURL);
                movie.setPosterURL(posterURL);
                return;
            } // Check www.impawards.com
            /*
             * else if (!(posterURL = this.testImpawardsPoster(movie.getId(FILMUPIT_PLUGIN_ID))).equalsIgnoreCase("Unknown")) {
             * logger.finest("Movie PosterURL : " + posterURL); movie.setPosterURL(posterURL); return; }
             */

            // Check www.moviecovers.com (if set in property file)
            else if ("moviecovers".equals(preferredPosterSearchEngine)
                            && !(posterURL = this.getPosterURLFromMoviecoversViaGoogle(movie.getTitle())).equalsIgnoreCase("Unknown")) {
                logger.finest("Movie PosterURL : " + posterURL);
                movie.setPosterURL(posterURL);
                return;
            } else {
                xml = webBrowser.request("http://www.imdb.com/title/" + movie.getId(FILMUPIT_PLUGIN_ID));
                int castIndex = xml.indexOf("<h3>Cast</h3>");
                int beginIndex = xml.indexOf("src=\"http://ia.media-imdb.com/images");
                if (beginIndex < castIndex && beginIndex != -1) {

                    StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 5), "\"");
                    posterURL = st.nextToken();
                    int index = posterURL.indexOf("_SY");
                    if (index != -1) {
                        posterURL = posterURL.substring(0, index) + "_SY800_SX600_.jpg";
                    }
                } else {
                    // try searching an alternate search engine
                    String alternateURL = "Unknown";
                    if ("google".equalsIgnoreCase(preferredPosterSearchEngine)) {
                        alternateURL = getPosterURLFromGoogle(movie.getTitle());
                    } else if ("yahoo".equalsIgnoreCase(preferredPosterSearchEngine)) {
                        alternateURL = getPosterURLFromYahoo(movie.getTitle());
                    }

                    if (!alternateURL.equalsIgnoreCase("Unknown")) {
                        posterURL = alternateURL;
                    }
                }
            }
            logger.finest("Movie PosterURL : " + posterURL);
            movie.setPosterURL(posterURL);
        } catch (Exception e) {
            logger.severe("Failed retreiving poster : " + pageUrl);
            e.printStackTrace();
        }
    }

    private int parseRating(String rating) {
        int index = rating.indexOf("etoile_");
        try {
            return (int)(Float.parseFloat(rating.substring(index + 7, index + 8)) / 4.0 * 100);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = true;
        try {
            String FilmUpITId = mediaFile.getId(FILMUPIT_PLUGIN_ID);
            if (FilmUpITId.equalsIgnoreCase(Movie.UNKNOWN)) {
                FilmUpITId = getFilmUpITId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile);
            }
            // we also get imdb Id for extra infos
            if (mediaFile.getId(IMDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(IMDB_PLUGIN_ID, getImdbId(mediaFile.getTitle(), mediaFile.getYear()));
                logger.finest("Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
            }
            if (!FilmUpITId.equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(FILMUPIT_PLUGIN_ID, FilmUpITId);
                if (mediaFile.isTVShow()) {
                    super.scan(mediaFile);
                } else {
                    retval = updateMovieInfo(mediaFile);
                }
            } else {
                // If no FilmUpITId found fallback to Imdb
                logger.finer("No Filmup Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (ParseException e) {
            // If no FilmUpITId found fallback to Imdb
            logger.finer("Parse error in FilmUpITPlugin we fall back to ImdbPlugin");
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the FilmUpITId matching the specified movie name. This routine is base on a FilmUpIT search.
     * 
     * @throws ParseException
     */
    private String getFilmUpITId(String movieName, String year, Movie mediaFile) throws ParseException {
        String FilmUpITId = Movie.UNKNOWN;

        try {
            StringBuffer sb = new StringBuffer("http://filmup.leonardo.it/cgi-bin/search.cgi?ps=10&fmt=long&q=");
            sb.append(URLEncoder.encode(movieName.replace(' ', '+'), "iso-8859-1"));
            sb.append("&ul=%25%2Fsc_%25&x=0&y=0&m=any&wf=0020&wm=wrd&sy=0");
            String xml = webBrowser.request(sb.toString());

            String FilmUpITStartResult;
            String FilmUpITMediaPrefix;
            FilmUpITStartResult = "<DT>1.";
            FilmUpITMediaPrefix = "sc_";

            for (String searchResult : extractTags(xml, FilmUpITStartResult, "<DD>", FilmUpITMediaPrefix, ".htm")) {
                // logger.finest("SearchResult = " + searchResult);
                return searchResult;
            }

            logger.finer("No ID Found with request : " + sb.toString());
            return Movie.UNKNOWN;

        } catch (Exception e) {
            logger.severe("Failed to retrieve FilmUp ID for movie : " + movieName);
            logger.severe("We fall back to ImdbPlugin");
            throw new ParseException(FilmUpITId, 0);
        }
    }

    protected String extractTag(String src, String tagStart, String tagEnd) {
        int beginIndex = src.indexOf(tagStart);
        if (beginIndex < 0) {
            // logger.finest("extractTag value= Unknown");
            return Movie.UNKNOWN;
        }
        try {
            String subString = src.substring(beginIndex + tagStart.length());
            int endIndex = subString.indexOf(tagEnd);
            if (endIndex < 0) {
                // logger.finest("extractTag value= Unknown");
                return Movie.UNKNOWN;
            }
            subString = subString.substring(0, endIndex);

            String value = HTMLTools.decodeHtml(subString.trim());
            // logger.finest("extractTag value=" + value);
            return value;
        } catch (Exception e) {
            logger.severe("extractTag an exception occurred during tag extraction : " + e);
            return Movie.UNKNOWN;
        }
    }

    protected String removeHtmlTags(String src) {
        return src.replaceAll("\\<.*?>", "");
    }

    protected String removeOpenedHtmlTags(String src) {
        String result = src.replaceAll("^.*?>", "");
        result = result.replaceAll("<.*?$", "");
        // logger.finest("removeOpenedHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
    }

    protected ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd, String startTag, String endTag) {
        ArrayList<String> tags = new ArrayList<String>();
        int index = src.indexOf(sectionStart);
        if (index == -1) {
            // logger.finest("extractTags no sectionStart Tags found");
            return tags;
        }
        index += sectionStart.length();
        int endIndex = src.indexOf(sectionEnd, index);
        if (endIndex == -1) {
            // logger.finest("extractTags no sectionEnd Tags found");
            return tags;
        }

        String sectionText = src.substring(index, endIndex);
        int lastIndex = sectionText.length();
        index = 0;
        int startLen = 0;
        int endLen = endTag.length();

        if (startTag != null) {
            index = sectionText.indexOf(startTag);
            startLen = startTag.length();
        }
        // logger.finest("extractTags sectionText = " + sectionText);
        // logger.finest("extractTags startTag = " + startTag);
        // logger.finest("extractTags startTag index = " + index);
        while (index != -1) {
            index += startLen;
            endIndex = sectionText.indexOf(endTag, index);
            if (endIndex == -1) {
                logger.finest("extractTags no endTag found");
                endIndex = lastIndex;
            }
            String text = sectionText.substring(index, endIndex);
            // logger.finest("extractTags Tag found text = [" + text+"]");

            // replaceAll used because trim() does not trim unicode space
            tags.add(HTMLTools.decodeHtml(text.trim()).replaceAll("^[\\s\\p{Zs}\\p{Zl}\\p{Zp}]*\\b(.*)\\b[\\s\\p{Zs}\\p{Zl}\\p{Zp}]*$", "$1"));
            endIndex += endLen;
            if (endIndex > lastIndex) {
                break;
            }
            if (startTag != null) {
                index = sectionText.indexOf(startTag, endIndex);
            } else {
                index = endIndex;
            }
        }
        return tags;
    }

    protected ArrayList<String> extractHtmlTags(String src, String sectionStart, String sectionEnd, String startTag, String endTag) {
        ArrayList<String> tags = new ArrayList<String>();
        int index = src.indexOf(sectionStart);
        if (index == -1) {
            // logger.finest("extractTags no sectionStart Tags found");
            return tags;
        }
        index += sectionStart.length();
        int endIndex = src.indexOf(sectionEnd, index);
        if (endIndex == -1) {
            // logger.finest("extractTags no sectionEnd Tags found");
            return tags;
        }

        String sectionText = src.substring(index, endIndex);
        int lastIndex = sectionText.length();
        index = 0;
        int endLen = endTag.length();

        if (startTag != null) {
            index = sectionText.indexOf(startTag);
        }
        // logger.finest("extractTags sectionText = " + sectionText);
        // logger.finest("extractTags startTag = " + startTag);
        // logger.finest("extractTags startTag index = " + index);
        while (index != -1) {
            endIndex = sectionText.indexOf(endTag, index);
            if (endIndex == -1) {
                endIndex = lastIndex;
            }
            endIndex += endLen;
            String text = sectionText.substring(index, endIndex);
            // logger.finest("extractTags Tag found text = " + text);
            tags.add(text);
            if (endIndex > lastIndex) {
                break;
            }
            if (startTag != null) {
                index = sectionText.indexOf(startTag, endIndex);
            } else {
                index = endIndex;
            }
        }
        return tags;
    }

    public void scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);

        // If we use FilmUpIT plugin look for
        // http://www.FilmUpIT.fr/...=XXXXX.html
        logger.finest("Scanning NFO for Filmup Id");
        int beginIndex = nfo.indexOf("http://filmup.leonardo.it/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf("sc_", beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf(".", beginIdIndex);
                if (endIdIndex != -1) {
                    logger.finer("Filmup Id found in nfo = " + nfo.substring(beginIdIndex + 3, endIdIndex));
                    movie.setId(FilmUpITPlugin.FILMUPIT_PLUGIN_ID, nfo.substring(beginIdIndex + 3, endIdIndex));
                } else {
                    logger.finer("No Filmup Id found in nfo !");
                }
            } else {
                logger.finer("No Filmup Id found in nfo !");
            }
        } else {
            logger.finer("No Filmup Id found in nfo !");
        }
    }
}
