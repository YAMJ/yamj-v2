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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.ImdbSiteDataDefinition;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;

public class ImdbInfo {
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private static final String DEFAULT_SITE = "us";
    private static final Map<String, ImdbSiteDataDefinition> matchesDataPerSite = new HashMap<String, ImdbSiteDataDefinition>();
    private final String imdbSite = PropertiesUtil.getProperty("imdb.site", DEFAULT_SITE);

    private String preferredSearchEngine;
    private WebBrowser webBrowser;

    private ImdbSiteDataDefinition siteDef;
    static {
        matchesDataPerSite.put("us", new ImdbSiteDataDefinition("http://www.imdb.com/", "ISO-8859-1", "Director", "Cast", "Release Date", "Runtime", "Country",
                        "Company", "Genre", "Quotes", "Plot", "Rated", "Certification", "Original Air Date", "Writer"));

        matchesDataPerSite.put("fr", new ImdbSiteDataDefinition("http://www.imdb.fr/", "ISO-8859-1", "R&#xE9;alisateur", "Ensemble", "Date de sortie", "Dur&#xE9;e", "Pays",
                        "Soci&#xE9;t&#xE9;", "Genre", "Citation", "Intrigue", "Rated", "Classification", "Date de sortie", "Sc&#xE9;naristes"));

        matchesDataPerSite.put("es", new ImdbSiteDataDefinition("http://www.imdb.es/", "ISO-8859-1", "Director", "Reparto", "Fecha de Estreno", "Duraci&#xF3;n", "Pa&#xED;s",
                        "Compa&#xF1;&#xED;a", "G&#xE9;nero", "Quotes", "Trama", "Rated", "Clasificaci&#xF3;n", "Fecha de Estreno", "Escritores"));

        matchesDataPerSite.put("de", new ImdbSiteDataDefinition("http://www.imdb.de/", "ISO-8859-1", "Regisseur", "Besetzung", "Premierendatum", "L&#xE4;nge", "Land",
                        "Firma", "Genre", "Quotes", "Handlung", "Rated", "Altersfreigabe", "Premierendatum", "Guionista"));

        matchesDataPerSite.put("it", new ImdbSiteDataDefinition("http://www.imdb.it/", "ISO-8859-1", "Regista|Registi", "Cast", "Data di uscita", "Durata",
                        "Nazionalit&#xE0;", "Compagnia", "Genere", "Quotes", "Trama", "Rated", "Certification", "Data di uscita", "Sceneggiatore"));

        matchesDataPerSite.put("pt", new ImdbSiteDataDefinition("http://www.imdb.pt/", "ISO-8859-1", "Diretor", "Elenco", "Data de Lan&#xE7;amento", "Dura&#xE7;&#xE3;o",
                        "Pa&#xED;s", "Companhia", "G&#xEA;nero", "Quotes", "Argumento", "Rated", "Certifica&#xE7;&#xE3;o", "Data de Lan&#xE7;amento",
                        "Roteirista"));
        
        // Use this as a workaround for English speakers abroad who get localised versions of imdb.com
        matchesDataPerSite.put("labs", new ImdbSiteDataDefinition("http://akas.imdb.com/", "ISO-8859-1", "Director|Directors", "Cast", "Release Date", "Runtime", "Country",
                        "Production Co", "Genres", "Quotes", "Storyline", "Rated", "Certification", "Original Air Date", "Writer|Writers"));
        
        // TODO: Leaving this as labs.imdb.com for the time being, but will be updated to www.imdb.com
        matchesDataPerSite.put("us2", new ImdbSiteDataDefinition("http://labs.imdb.com/", "ISO-8859-1", "Director|Directors", "Cast", "Release Date", "Runtime", "Country",
                        "Production Co", "Genres", "Quotes", "Storyline", "Rated", "Certification", "Original Air Date", "Writer|Writers"));

        // Not 100% sure these are correct
        matchesDataPerSite.put("it2", new ImdbSiteDataDefinition("http://www.imdb.it/", "ISO-8859-1", "Regista|Registi", "Attori", "Data di uscita", "Durata",
                        "Nazionalit&#xE0;", "Compagnia", "Genere", "Quotes", "Trama", "Rated", "Certification", "Data di uscita", "Sceneggiatore"));
    }

    public void setPreferredSearchEngine(String preferredSearchEngine) {
        this.preferredSearchEngine = preferredSearchEngine;
    }

    public ImdbInfo() {
        webBrowser = new WebBrowser();

        preferredSearchEngine = PropertiesUtil.getProperty("imdb.id.search", "imdb");
        siteDef = matchesDataPerSite.get(imdbSite);
        if (siteDef == null) {
            logger.warning("ImdbInfo: No site definition for " + imdbSite + " using the default instead " + DEFAULT_SITE);
            siteDef = matchesDataPerSite.get(DEFAULT_SITE);
        }
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This routine is based on a IMDb request.
     */
    public String getImdbId(String movieName, String year) {
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
     * @param movieName
     *            The name of the Movie to search for
     * @param year
     *            The year of the movie
     * @return The IMDb Id if it was found
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

        } catch (Exception error) {
            logger.severe("ImdbInfo Failed retreiving IMDb Id for movie : " + movieName);
            logger.severe("ImdbInfo Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This routine is base on a Google request.
     * 
     * @param movieName
     *            The name of the Movie to search for
     * @param year
     *            The year of the movie
     * @return The IMDb Id if it was found
     */
    private String getImdbIdFromGoogle(String movieName, String year) {
        try {
            logger.finer("ImdbInfo querying Google for " + movieName);

            StringBuffer sb = new StringBuffer("http://www.google.com/search?q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }

            sb.append("+site%3Awww.imdb.com&meta=");

            logger.finest("ImdbInfo Google search: " + sb.toString());
            
            String xml = webBrowser.request(sb.toString());
            int beginIndex = xml.indexOf("/title/tt");
            StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 7), "/\"");
            String imdbId = st.nextToken();

            if (imdbId.startsWith("tt")) {
                logger.finer("Found IMDb ID: " + imdbId);
                return imdbId;
            } else {
                return Movie.UNKNOWN;
            }

        } catch (Exception error) {
            logger.severe("ImdbInfo Failed retreiving IMDb Id for movie : " + movieName);
            logger.severe("ImdbInfo Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /**
     * Retrieve the IMDb matching the specified movie name and year. This routine is base on a IMDb request.
     */
    private String getImdbIdFromImdb(String movieName, String year) {
        /*
         * IMDb matches seem to come in several "flavours". Firstly, if there is one exact match it returns the matching IMDb page. If that fails to produce an
         * unique hit then a list of possible matches are returned categorised as: Popular Titles (Displaying ? Results) Titles (Exact Matches) (Displaying ?
         * Results) Titles (Partial Matches) (Displaying ? Results)
         * 
         * We should check the Exact match section first, then the poplar titles and finally the partial matches. Note: That even with exact matches there can
         * be more than 1 hit, for example "Star Trek"
         */
        
        //logger.fine("Movie Name: " + movieName + " - " + year);

        try {
            StringBuffer sb = new StringBuffer(siteDef.getSite() + "find?q=");
            sb.append(URLEncoder.encode(movieName, "iso-8859-1"));

            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+%28").append(year).append("%29");
            }
            sb.append(";s=tt;site=aka");

            logger.finest("ImdbInfo Querying IMDB for " + sb.toString());
            String xml = webBrowser.request(sb.toString());

            // Check if this is an exact match (we got a movie page instead of a results list)
            Pattern titleregex = Pattern.compile(Pattern.quote("<link rel=\"canonical\" href=\"" + siteDef.getSite() + "title/") + "(tt\\d+)/\"");
            Matcher titlematch = titleregex.matcher(xml);
            if (titlematch.find()) {
               // logger.finer("ImdbInfo direct found " + titlematch.group(1));
               return titlematch.group(1);
            }
            
            String otherMovieName = HTMLTools.extractTag(HTMLTools.extractTag(xml, ";ttype=ep\">", "\"</a>.</li>"), "<b>" , "</b>").toLowerCase();
            String formattedMovieName;
            if (!otherMovieName.equals("")) {
                if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN) && otherMovieName.endsWith(")") && otherMovieName.contains("(")) {
                    otherMovieName = otherMovieName.substring(0,otherMovieName.lastIndexOf("(")-1);
                    formattedMovieName = otherMovieName + "</a> (" + year + ")";
                } else {
                    formattedMovieName = otherMovieName + "</a>";
                }
            } else {
                sb = new StringBuffer(URLEncoder.encode(movieName, "iso-8859-1").replace("+"," ")+"</a>");
                if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                    sb.append(" (").append(year).append(")");
                }
                otherMovieName = sb.toString();
                formattedMovieName = otherMovieName;
            }
            
            //logger.fine("ImdbInfo title search : " + formattedMovieName);
            for (String searchResult : HTMLTools.extractTags(xml, "<div class=\"media_strip_thumbs\">", "<div id=\"sidebar\">", ".src='/rg/find-title-", "</td>", false)) {
                //logger.fine("ImdbInfo title check : " + searchResult);
                if (searchResult.toLowerCase().indexOf(formattedMovieName) != -1) {
                    //logger.fine("ImdbInfo title match : " + searchResult);
                    return HTMLTools.extractTag(searchResult, "/images/b.gif?link=/title/", "/';\">");
                } else {
                    for (String otherResult : HTMLTools.extractTags(searchResult, "</';\">", "</p>", "<p class=\"find-aka\">", "</em>", false)) {
                        if (otherResult.toLowerCase().indexOf("\"" + otherMovieName + "\"") != -1) {
                            //logger.fine("ImdbInfo othertitle match : " + otherResult);
                            return HTMLTools.extractTag(searchResult, "/images/b.gif?link=/title/", "/';\">");
                        }
                    }
                }
            }
            
            // If we don't have an ID try google
            logger.finer("Failed to find an exact match on IMDb, trying Google");
            return getImdbIdFromGoogle(movieName, year);
/*            
            // Check if first "popular title" match
            titleregex = Pattern.compile(Pattern.quote("find-title-1/title_popular/images/b.gif?link=/title/") + "(tt\\d+)" + Pattern.quote("/';\">"+ sb));
            titlematch = titleregex.matcher(xml);
            logger.finer("ImdbInfo compare first \"popular title\" match " + titleregex);
            if (titlematch.find()) {
                logger.finer("ImdbInfo first \"popular title\" match " + titlematch.group(1));
               return titlematch.group(1);
            }
            
            // Check for an exact match in the results list, based on given name better as the "exact titles" list
//            titleregex = Pattern.compile("title_exact/images/b.gif\\?link=/title/(tt\\d+)/';\">"+ sb);
            titleregex = Pattern.compile(Pattern.quote("title_exact/images/b.gif?link=/title/") + "(tt\\d+)" + Pattern.quote("/';\">"+ sb));
            // Pattern titleregex = Pattern.compile("/images/b.gif\\?link=/title/(tt\\d+)/';\">"+ movieName +"</a>");
            titlematch = titleregex.matcher(xml);
            logger.finer("ImdbInfo search \"exact title\" matches " + titleregex);
            if (titlematch.find()) {
                logger.finer("ImdbInfo \"exact title\" match " + titlematch.group(1));
                return titlematch.group(1);
            }
*/
            /*
             * This is what the wiki says imdb.perfect.match is supposed to do, but the result doesn't make a lot of sense to me. See the discussion for issue
             * 567. if (perfectMatch) { // Reorder the titles so that the "Exact Matches" section gets scanned first int exactStart =
             * xml.indexOf("Titles (Exact Matches)"); if (-1 != exactStart) { int exactEnd = xml.indexOf("Titles (Partial Matches)"); if (-1 == exactEnd) {
             * exactEnd = xml.indexOf("Titles (Approx Matches)"); } if (-1 != exactEnd) { xml = xml.substring(exactStart, exactEnd) + xml.substring(0,
             * exactStart) + xml.substring(exactEnd); } else { xml = xml.substring(exactStart) + xml.substring(0, exactStart); } } }
             */

 //           return searchForTitle(xml, movieName);
        } catch (Exception error) {
            logger.severe("ImdbInfo Failed retreiving IMDb Id for movie : " + movieName);
            logger.severe("ImdbInfo Error : " + error.getMessage());
        }

        return Movie.UNKNOWN;
    }

    @SuppressWarnings("unused")
    private String searchForTitle(String imdbXML, String movieName) {
        Pattern imdbregex = Pattern.compile("\\<a(?:\\s*[^\\>])\\s*" // start of a-tag, and cruft before the href
                        + "href=\"/title/(tt\\d+)" // the href, grab the id
                        + "(?:\\s*[^\\>])*\\>" // cruft after the href, to the end of the a-tag
                        + "([^\\<]+)\\</a\\>" // grab link text (ie, title), match to the close a-tag
                        + "\\s*" + "\\((\\d{4})(?:/[^\\)]+)?\\)" // year, eg (1999) or (1999/II), grab the 4-digit year only
                        + "\\s*" + "((?:\\(VG\\))?)" // video game flag (if present)
        );
        // Groups: 1=id, 2=title, 3=year, 4=(VG)

        Matcher match = imdbregex.matcher(imdbXML);
        while (match.find()) {
            // Find the first title where the year matches (if present) and that isn't a video game
            if (!"(VG)".equals(match.group(4))
            // Don't worry about matching title/year info -- IMDB took care of that already.
            // && (null == year || Movie.UNKNOWN == year || year.equals(match.group(3)))
            // && (!perfectMatch || movieName.equalsIgnoreCase(match.group(2)))
            ) {
                logger.finest("ImdbInfo: " + movieName + ": found IMDB match, " + match.group(2) + " (" + match.group(3) + ") " + match.group(4));
                return match.group(1);
            } else {
                logger.finest("ImdbInfo: " + movieName + ": rejected IMDB match " + match.group(2) + " (" + match.group(3) + ") " + match.group(4));
            }
        }

        // Return UNKNOWN if the match isn't found
        return Movie.UNKNOWN;
    }

    public ImdbSiteDataDefinition getSiteDef() {
        return siteDef;
    }
    
    /**
     * Get a specific site definition from the list
     * @param requiredSiteDef
     * @return The Site definition if found, null otherwise
     */
    public ImdbSiteDataDefinition getSiteDef(String requiredSiteDef) {
        return matchesDataPerSite.get(requiredSiteDef);
    }

    public String getPreferredSearchEngine() {
        return preferredSearchEngine;
    }

    public String getImdbSite() {
        return imdbSite;
    }
    
}
