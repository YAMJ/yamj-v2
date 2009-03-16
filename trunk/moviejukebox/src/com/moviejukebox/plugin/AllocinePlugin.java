package com.moviejukebox.plugin;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class AllocinePlugin extends ImdbPlugin {

    public static String ALLOCINE_PLUGIN_ID = "allocine";
    private static int ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT = 600;

    public AllocinePlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "France");
    }

    /**
     * Scan Allocine html page for the specified Tv Show
     */
    protected void updateTVShowInfo(Movie movie) {
        try {
            String xml = webBrowser.request("http://www.allocine.fr/series/ficheserie_gen_cserie=" + movie.getId(ALLOCINE_PLUGIN_ID) + ".html");

            if (!movie.isOverrideTitle()) {
                movie.setTitle(extractTag(xml, "<h1 style='color:#D20000;font-weight:bold'>", "</h1>"));
            }
            movie.setRating(parseRating(extractTag(xml, "<h4>Note moyenne :", "</h4>")));
            String tmpPlot = removeHtmlTags(extractTag(xml, "Synopsis</span>&nbsp;&nbsp;&nbsp;", "</h5>"));
            // limit plot to ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT char
            if (tmpPlot.length() > ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT) {
                tmpPlot = tmpPlot.substring(0, Math.min(tmpPlot.length(), ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT)) + "...";
            }
            movie.setPlot(tmpPlot);
            movie.setDirector(removeHtmlTags(extractTag(xml, "<h4>Série créée par ", "</a> en")));
            movie.setRuntime(extractTag(xml, "Format</span> : ", "."));
            movie.setCountry(extractTag(xml, "Nationalité</span> :", "</h5>"));

            for (String genre : extractTags(xml, "Genre</span> :", "-", " ", ",")) {
                movie.addGenre(Library.getIndexingGenre(removeOpenedHtmlTags(genre)));
            }

            // movie.setCertification(getPreferredValue(extractTags(xml,
            // "<h5>Certification:</h5>", "</div>",
            // "<a href=\"/List?certificates=", "</a>")));

            if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase("Unknown")) {

                movie.setYear(extractTag(xml, "</a> en", "</h4>"));
            }

            // Get Fanart
            if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
            	movie.setFanartURL(getFanartURL(movie));
            	if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            		movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
            	}
            }
			
            for (String acteur : extractTags(xml, "<h4>Avec", "</h4>", "personne/fichepersonne_gen_cpersonne", "</a>")) {
                movie.addActor(removeOpenedHtmlTags(acteur));
            }

            updatePoster(movie);
            scanTVShowTitles(movie);
        } catch (Exception e) {
            logger.severe("Failed retreiving alloCine TVShow info for " + movie.getId(ALLOCINE_PLUGIN_ID));
            e.printStackTrace();
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
            // "Start a new request for seasons details : http://www.allocine.fr/series/episodes_gen_cserie="
            // + movie.getId(ALLOCINE_PLUGIN_ID) + ".html");
            String xml = webBrowser.request("http://www.allocine.fr/series/episodes_gen_cserie=" + allocineId + ".html");

            for (String seasonTag : extractHtmlTags(xml, "Sélectionner un numéro de saison", "<table", "<a href=\"/series/episodes_gen_csaison", "</a>")) {
                try {
                    String seasonIdString = removeHtmlTags(seasonTag);
                    // logger.finest("New Season detected = " + seasonIdString);
                    // logger.finest("New Season detected = " + seasonIdString.substring(7,
                    // seasonIdString.length()));
                    int seasonId = Integer.valueOf(seasonIdString.substring(7, seasonIdString.length()));
                    // logger.finest("New Season detected = " + seasonId);
                    String seasonAllocineId = extractTag(seasonTag, "/series/episodes_gen_csaison=", "&");
                    // logger.finest("Season Id = " + seasonAllocineId);
                    // logger.finest("Season IdI = " + seasonId + ", movie.getSeason() = " +
                    // movie.getSeason());
                    if (seasonId == movie.getSeason()) {
                        // we found the right season, time to get the infos
                        // logger.finest("The right Season IdI = " + seasonId);
                        xml = webBrowser.request("http://www.allocine.fr/series/episodes_gen_csaison=" + seasonAllocineId + "&cserie=" + allocineId + ".html");
                        for (MovieFile file : movie.getFiles()) {
                            if (!file.isNewFile() || file.hasTitle()) {
                                // don't scan episode title if it exists in XML data
                                continue;
                            }
                            StringBuilder sb = new StringBuilder();
                            boolean first = true;
                            for (int episode = file.getFirstPart(); episode < file.getLastPart(); ++episode) {
                                String episodeName = removeHtmlTags(extractTag(xml, "<b>Episode " + episode + "</b></h4>&nbsp;-&nbsp;", "<span id="));
                                if (first) {
                                    first = false;
                                } else {
                                    sb.append(" / ");
                                }
                                sb.append(episodeName);
                            }
                            file.setTitle(sb.toString());
                        }
                    }
                } catch (Exception e) {
                    // logger.severe("Error while getting season infos " + e);
                    // nothing to do, we skip this season
                }
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving episodes titles for movie : " + movie.getTitle());
            logger.severe("Error : " + e.getMessage());
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
                movie.setTitle(extractTag(xml, "<h1 class=\"TitleFilm\">", "</h1>"));
            }
            
            if (movie.getOriginalTitle().equalsIgnoreCase(Movie.UNKNOWN)){
            	movie.setOriginalTitle(removeHtmlTags(extractTag(xml, "Titre original : <i>", "</i>")));
            }
            
            if (movie.getRating() == -1){
            	movie.setRating(parseRating(extractTag(xml, "<h4>Note moyenne :", "</h4>")));
            }

            if (movie.getPlot().equalsIgnoreCase(Movie.UNKNOWN)){
            	// limit plot to ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT char
            	String tmpPlot = removeHtmlTags(extractTag(xml, "<h2 class=\"SpBlocTitle\" >Synopsis", "</h4>"));
            	if (tmpPlot.length() > ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT) {
            		tmpPlot = tmpPlot.substring(0, Math.min(tmpPlot.length(), ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT)) + "...";
            	}
            	movie.setPlot(tmpPlot);
            }

            if (movie.getDirector().equalsIgnoreCase(Movie.UNKNOWN)){
            	movie.setDirector(removeHtmlTags(extractTag(xml, "<h3 class=\"SpProse\">Réalisé par ", "</h3>")));
//            logger.finest("Movie Director = " + movie.getDirector());
            }
            
            if (movie.getReleaseDate().equals(Movie.UNKNOWN)) {
            	movie.setReleaseDate(removeHtmlTags(extractTag(xml, "Date de sortie : <b>", "</b>")));
            }
            
            if (movie.getRuntime().equals(Movie.UNKNOWN)) {
            	movie.setRuntime(extractTag(xml, "Durée : ", "."));
//            logger.finest("Durée = " + movie.getRuntime());
            }

            if (movie.getCountry().equals(Movie.UNKNOWN)) {
            	movie.setCountry(extractTag(xml, "<h3 class=\"SpProse\">Film", "."));
            	//            logger.finest("Movie Country = " + movie.getCountry());
            }

            if (movie.getCompany().equals(Movie.UNKNOWN)) {
            	movie.setCompany(removeHtmlTags(extractTag(xml, "Distribué par ", "</h3>")));
            }

            if (movie.getGenres().isEmpty()) {
            	for (String genre : extractTags(xml, "Genre : ", "</h3>", "film/alaffiche_genre_gen_genre", "</a>")) {
            		movie.addGenre(removeOpenedHtmlTags(genre));
            	}
            }
            // movie.setCertification(getPreferredValue(extractTags(xml,
            // "<h5>Certification:</h5>", "</div>",
            // "<a href=\"/List?certificates=", "</a>")));

            if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
            	movie.setYear(extractTag(xml, "Année de production : ", "</h3>"));
            }

            // Get Fanart
            if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
            	movie.setFanartURL(getFanartURL(movie));
            	if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
            		movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
            	}
            }

            if (movie.getCast().isEmpty()) {
            	for (String acteur : extractTags(xml, "<h3 class=\"SpProse\">Avec ", "</h3>", "personne/fichepersonne_gen_cpersonne", "</a>")) {
            		movie.addActor(removeOpenedHtmlTags(acteur));
            	}
            }
            updatePoster(movie);

        } catch (IOException e) {
            logger.severe("Failed retreiving allocine infos for movie : " + movie.getId(ALLOCINE_PLUGIN_ID));
            e.printStackTrace();
        }
        return true;
    }

    private void updatePoster(Movie movie) {
        // make an Imdb request for poster
        if (movie.getPosterURL() != null && !movie.getPosterURL().equalsIgnoreCase("Unknown")) {
            // we already have a poster URL
            logger.finer("Movie already has PosterURL : " + movie.getPosterURL());
            return;
        }
        try {
            String posterURL = "Unknown";
            String xml;
            // Check alloCine first only for movies because TV Show posters are
            // wrong.
            if (!movie.isTVShow()) {
                String baseUrl = "http://www.allocine.fr/film/galerievignette_gen_cfilm=";
                xml = webBrowser.request(baseUrl + movie.getId(ALLOCINE_PLUGIN_ID) + ".html");
                posterURL = extractTag(xml, "img id='imgNormal' class='photo' src='", "'");
                if (!posterURL.equalsIgnoreCase("Unknown")) {
                    logger.finest("Movie PosterURL : " + posterURL);
                    movie.setPosterURL(posterURL);
                    return;
                }
            }
            // Check posters.motechnet.com
            if (this.testMotechnetPoster(movie.getId(IMDB_PLUGIN_ID))) {
                posterURL = "http://posters.motechnet.com/covers/" + movie.getId(IMDB_PLUGIN_ID) + "_largeCover.jpg";
                logger.finest("Movie PosterURL : " + posterURL);
                movie.setPosterURL(posterURL);
                return;
            } // Check www.impawards.com
            else if (!(posterURL = this.testImpawardsPoster(movie.getTitle(), movie.getYear())).equalsIgnoreCase("Unknown")) {
                logger.finest("Movie PosterURL : " + posterURL);
                movie.setPosterURL(posterURL);
                return;
            } // Check www.moviecovers.com (if set in property file)
            else if ("moviecovers".equals(preferredPosterSearchEngine)
                            && !(posterURL = this.getPosterURLFromMoviecoversViaGoogle(movie.getTitle())).equalsIgnoreCase("Unknown")) {
                logger.finest("Movie PosterURL : " + posterURL);
                movie.setPosterURL(posterURL);
                return;
            } else {
                xml = webBrowser.request("http://www.imdb.com/title/" + movie.getId(IMDB_PLUGIN_ID));
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
            logger.severe("Failed retreiving poster for movie : " + movie.getId(ALLOCINE_PLUGIN_ID));
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
            String allocineId = mediaFile.getId(ALLOCINE_PLUGIN_ID);
            if (allocineId.equalsIgnoreCase(Movie.UNKNOWN)) {
                allocineId = getAllocineId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile);
            }
            // we also get imdb Id for extra infos
            if (mediaFile.getId(IMDB_PLUGIN_ID).equalsIgnoreCase(Movie.UNKNOWN)) {
                mediaFile.setId(IMDB_PLUGIN_ID, getImdbId(mediaFile.getTitle(), mediaFile.getYear()));
                logger.finest("Found imdbId = " + mediaFile.getId(IMDB_PLUGIN_ID));
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
                logger.finer("No Allocine Id available, we fall back to ImdbPlugin");
                retval = super.scan(mediaFile);
            }
        } catch (ParseException e) {
            // If no AllocineId found fallback to Imdb
            logger.finer("Parse error in AllocinePlugin we fall back to ImdbPlugin");
            retval = super.scan(mediaFile);
        }
        return retval;
    }

    /**
     * retrieve the allocineId matching the specified movie name. This routine is base on a alloCine search.
     * 
     * @throws ParseException
     */
    private String getAllocineId(String movieName, String year, Movie mediaFile) throws ParseException {
        String allocineId = Movie.UNKNOWN;
        try {
            StringBuffer sb = new StringBuffer("http://www.allocine.fr/recherche/?motcle=");
            sb.append(URLEncoder.encode(movieName.replace(' ', '+'), "iso-8859-1"));
            sb.append("&x=0&y=0&rub=0"); // some AlloCine Magic
            // logger.finest("Allocine request : "+sb.toString());
            String xml = webBrowser.request(sb.toString());

            String alloCineStartResult;
            String alloCineMediaPrefix;
            if (mediaFile.isTVShow()) {
                alloCineStartResult = "Séries TV <h4>";
                alloCineMediaPrefix = "/series/ficheserie_gen_cserie=";
            } else {
                alloCineStartResult = "Films <h4>";
                alloCineMediaPrefix = "/film/fichefilm_gen_cfilm=";
            }

            for (String searchResult : extractTags(xml, alloCineStartResult, "<script type=\"text/javascript\">", alloCineMediaPrefix, "</h4></div>")) {
                // logger.finest("AlloCine SearchResult = " + searchResult);
                String searchResultYear = searchResult.substring(searchResult.lastIndexOf(">") + 1, searchResult.length());
                // logger.finest("AlloCine searchResultYear = " + searchResultYear);
                if (year == null || year.equalsIgnoreCase(Movie.UNKNOWN) || year.equalsIgnoreCase(searchResultYear)) {
                    int allocineIndexBegin = 0;
                    int allocineIndexEnd = searchResult.indexOf(".html");

                    allocineId = searchResult.substring(allocineIndexBegin, allocineIndexEnd);
                    // validate that allocineId is an int
                    Integer.valueOf(allocineId);
                    logger.finer("Found AllocineId = " + allocineId);
                    return allocineId;
                }
            }
            logger.finer("No AllocineId Found with request : " + sb.toString());
            return Movie.UNKNOWN;
        } catch (Exception e) {
            logger.severe("Failed to retrieve alloCine Id for movie : " + movieName);
            logger.severe("We fall back to ImdbPlugin");
            throw new ParseException(allocineId, 0);
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
        String result = src.replaceAll("\\<.*?>", "").trim();
//        logger.finest("removeHtmlTags before=[" + src + "], after=["+ result + "]");
        return result;
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
                // logger.finest("extractTags no endTag found");
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

        // If we use allocine plugin look for
        // http://www.allocine.fr/...=XXXXX.html
        logger.finest("Scanning NFO for Allocine Id");
        int beginIndex = nfo.indexOf("http://www.allocine.fr/");
        if (beginIndex != -1) {
            int beginIdIndex = nfo.indexOf("=", beginIndex);
            if (beginIdIndex != -1) {
                int endIdIndex = nfo.indexOf(".", beginIdIndex);
                if (endIdIndex != -1) {
                    logger.finer("Allocine Id found in nfo = " + nfo.substring(beginIdIndex + 1, endIdIndex));
                    movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, nfo.substring(beginIdIndex + 1, endIdIndex));
                } else {
                    logger.finer("No Allocine Id found in nfo !");
                }
            } else {
                logger.finer("No Allocine Id found in nfo !");
            }
        } else {
            logger.finer("No Allocine Id found in nfo !");
        }
    }
}
