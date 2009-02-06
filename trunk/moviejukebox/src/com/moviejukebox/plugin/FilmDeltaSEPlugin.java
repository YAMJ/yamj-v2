/* Filmdelta.se plugin
 * 
 * Contains code for an alternate plugin for fetching information on 
 * movies in swedish
 * 
 */

package com.moviejukebox.plugin;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.StringTokenizer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;


/**
 * Plugin to retrieve movie data from Swedish movie database www.filmdelta.se
 * Modified from imdb plugin and Kinopoisk plugin written by Yury Sidorov.
 * 
 * @author  johan.klinge
 * @version 0.2, 6th February 2009
 */
public class FilmDeltaSEPlugin extends ImdbPlugin {

    public static String FILMDELTA_PLUGIN_ID = "filmdelta";
    //Define plot length
    int preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty("filmdelta.plot.maxlength", "400"));
    String preferredRating = PropertiesUtil.getProperty("filmdelta.rating", "filmdelta");
    protected TheTvDBPlugin tvdb;

    public FilmDeltaSEPlugin() {
        super();
        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "Sweden");
        tvdb = new TheTvDBPlugin(); 
        logger.finest("Filmdelta plugin initialiserad OK");
    }

    @Override
    public boolean scan(Movie mediaFile) {
    	logger.finest("..starting scan");
        boolean retval = true;
        String filmdeltaId = mediaFile.getId(FILMDELTA_PLUGIN_ID);
        if (filmdeltaId == null || filmdeltaId.equalsIgnoreCase(Movie.UNKNOWN)) { 
            filmdeltaId = getFilmdeltaId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.getSeason());
            mediaFile.setId(FILMDELTA_PLUGIN_ID, filmdeltaId);
            // Get base info from imdb or tvdb
            if (!mediaFile.isTVShow())  
            	super.scan(mediaFile);
            else
            	tvdb.scan(mediaFile);
        }
        else {
        	// If ID is specified in NFO, set original title to unknown
        	mediaFile.setTitle(Movie.UNKNOWN);
        }
        //only scan filmdelta if a valid filmdeltaId was found
        //and the movie is not a tvshow
        if (filmdeltaId != null && 
        		!filmdeltaId.equalsIgnoreCase(Movie.UNKNOWN)&& 
        		!mediaFile.isTVShow()) 
        {
        	// Replace some movie data by data from Filmdelta 
            retval = updateFilmdeltaMediaInfo(mediaFile, filmdeltaId);
        }
        return retval;
    }

    /* Find id from url in nfo. Format:
     *  - http://www.filmdelta.se/filmer/<digits>/<movie_name>/ OR
     *  - http://www.filmdelta.se/prevsearch/<text>/filmer/<digits>/<movie_name>
     */
    @Override
    public void scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);
        logger.finest("Scanning NFO for Filmdelta Id");
        
        int beginIndex = nfo.indexOf("www.filmdelta.se/prevsearch");
        if (beginIndex != -1) {
        	beginIndex = beginIndex + 27;
            String filmdeltaId = makeFilmDeltaId(nfo, beginIndex, 2);
            movie.setId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID, filmdeltaId);
            logger.finest("Filmdelta Id found in nfo = " + movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
        } else if (nfo.indexOf("www.filmdelta.se/filmer") != -1 ) {
        	beginIndex = nfo.indexOf("www.filmdelta.se/filmer") + 24;
        	String filmdeltaId = makeFilmDeltaId(nfo, beginIndex, 0);
            movie.setId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID, filmdeltaId);
            logger.finest("Filmdelta Id found in nfo = " + movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
        } else {  
        	logger.finer("No Filmdelta Id found in nfo!");
        }
    }

     /**
     * retrieve FilmDeltaID matching the specified movie name and year. This routine is based on a 
     * google request.
     */
    private String getFilmdeltaId(String movieName, String year, int season) {
    	logger.finest("..searching Google to get FilmDeltaID");
        try {
            StringBuffer sb = new StringBuffer("http://www.google.se/search?hl=sv&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));
            if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
                sb.append("+").append(year);
            }
            sb.append(URLEncoder.encode("+site:filmdelta.se/filmer", "UTF-8"));

            String xml = webBrowser.request(sb.toString());

            int beginIndex = xml.indexOf("www.filmdelta.se/filmer/")+24;
        	String filmdeltaId = makeFilmDeltaId(xml, beginIndex, 0);
            logger.finest("FilmdeltaID = " + filmdeltaId);
            if (filmdeltaId != "") {
                return filmdeltaId;
            } else {
                return Movie.UNKNOWN;
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving Filmdelta Id for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }
    
    /* Utility method to make a filmdelta id from a string containing a 
     * filmdelta url
     * @author
     */
	private String makeFilmDeltaId(String nfo, int beginIndex, int skip) {
		StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex), "/");
		for(int i = 0; i < skip; i++ ) {
			st.nextToken();	
		}
		String filmdeltaId = st.nextToken() + "/" + st.nextToken();
		return filmdeltaId;
	}

    /**
     * Scan Filmdelta html page for the specified movie
     */
    private boolean updateFilmdeltaMediaInfo(Movie movie, String filmdeltaId) {
    	logger.finest("..starting filmdelta scan");
        try {
        	logger.finest("searchstring: " + "http://www.filmdelta.se/filmer/" + filmdeltaId);
            String xml = webBrowser.request("http://www.filmdelta.se/filmer/" + filmdeltaId + "/");
            logger.finest("result from filmdelta: " + xml);
            
            // Title
            if (!movie.isOverrideTitle()) {
            	String newTitle = HTMLTools.extractTag(xml, "title>", 0, "-");
            	String originalTitle = HTMLTools.extractTag(xml, "riginaltitel</h4>", 2);
            	logger.finest("Scraped title: " + newTitle);
            	logger.finest("Scraped original title: " + originalTitle);
            	if (!newTitle.equals(Movie.UNKNOWN)) {
                    movie.setTitle(newTitle.trim());
            	}
            	if(!originalTitle.equals(Movie.UNKNOWN)) {
            		movie.setOriginalTitle(originalTitle);
            	}
            }
            
            // Plot
            String plot = HTMLTools.extractTag(xml, "<div class=\"text\">", 2);
            logger.finest("Scraped Plot: " + plot);
            if (!plot.equals(Movie.UNKNOWN)) {
            	if (plot.length() > preferredPlotLength) { 
            		plot = plot.substring(0, preferredPlotLength) + "...";
            	}
            	movie.setPlot(plot);
            }
            
            // Genres - prefer imdb
        	if(movie.getGenres().isEmpty()) {
        		LinkedList<String> newGenres = new LinkedList<String>();            
            
        		ArrayList<String> filmdeltaGenres = HTMLTools.extractTags(xml, "<h4>Genre</h4>", "</div>", "<h5>", "</h5>");
        		for (String genre : filmdeltaGenres) {
        			if(genre.length() > 0) {
        				genre = genre.substring(0, genre.length()-5);
        				newGenres.add(genre);
        			}
        		}           
        		if (!newGenres.isEmpty()) {
        			movie.setGenres(newGenres);
        			logger.finest("Scraped genres: " + movie.getGenres().toString());
        		}
        	}
        	
            // Directors - prefer imdb
        	if(movie.getDirector().equals(Movie.UNKNOWN)) {
        		ArrayList<String> filmdeltaDirectors = HTMLTools.extractTags(xml, "<h4>Regiss&ouml;r</h4>", "</div>", "<h5>", "</h5>");
        		String newDirector = "";
        		if(!filmdeltaDirectors.isEmpty()) {
        			for(String dir : filmdeltaDirectors) {
        				dir = dir.substring(0, dir.length()-4);
        				newDirector = newDirector + dir + " / ";
        			}
        			newDirector = newDirector.substring(0, newDirector.length()-3);
        			movie.setDirector(newDirector);
        			logger.finest("Scraped director: " + movie.getDirector());	
        		}
        	}
            
            
            // Cast - prefer imdb
            if(movie.getCast().isEmpty()) {
            	Collection<String> newCast = new ArrayList<String>();
            
            	for (String actor : HTMLTools.extractTags(xml, "<h4>Sk&aring;despelare</h4>", "</div>", "<h5>", "</h5>")) {
            		String[] newActor = actor.split("</a>");
            		newCast.add(newActor[0]);
            	}
            	if (newCast.size() > 0) { 
            		movie.setCast(newCast);
            		logger.finest("Scraped actor: " + movie.getCast().toString());
            	}
            }

            // Country
            String country = HTMLTools.extractTag(xml, "Land, &aring;r, l&auml;ngd", 3);
            movie.setCountry(country);
            logger.finest("Scraped country: " + movie.getCountry());
            
            //Year
            String year = HTMLTools.extractTag(xml, "Land, &aring;r, l&auml;ngd", 5);
            String[] newYear = year.split("\\s");
            movie.setYear(newYear[1]);
            logger.finest("Scraped year: " + movie.getYear());
            
            // Rating
            String rating = HTMLTools.extractTag(xml, "style=\"margin-top:2px; font-weight:bold;\">", 8, "<");
            int newRating = 0;
            if(rating != Movie.UNKNOWN && (rating.indexOf("Snitt") != -1)) {
            	String[] result = rating.split(":");
            	rating = result[result.length-1];
            	logger.finest("filmdelta rating: " + newRating);
            	//multiply by 20 to make comparable to IMDB-ratings
            	newRating = (int)(Float.parseFloat(rating)*20);
            }
            if (preferredRating.equals("filmdelta")) {
            	//fallback to imdb if no filmdelta rating is available
            	if(newRating != 0)
            		movie.setRating(newRating);
            } else if (preferredRating.equals("average")) {
            	//don't count average rating if filmdelta=0
            	if(newRating != 0) {
            		newRating = (newRating + movie.getRating())/2;
            		movie.setRating(newRating);
            	}
               	
            }
            logger.finest("Movie.getRating: " + movie.getRating());
            
            // Poster - get from CDON.se
            String posterURL = getCDONPosterURL(movie.getTitle(), movie.getSeason());
            if(!posterURL.equals(Movie.UNKNOWN)) {
            	movie.setPosterURL(posterURL);
            }

            // Run time
            String runtime = HTMLTools.extractTag(xml, "Land, &aring;r, l&auml;ngd", 7);
            String[] newRunTime = runtime.split("\\s");
            if (newRunTime.length > 2) {
            	movie.setRuntime(newRunTime[1]);
            	logger.finest("Scraped runtime: " + movie.getRuntime());            	
            }
            
        } catch (Exception e) {
            logger.severe("Failed retreiving movie data from filmdelta.se : " + filmdeltaId);
            e.printStackTrace();
        }
        return true;
    }

	private String getCDONPosterURL(String movieName, int season) {
		String cdonPosterURL = Movie.UNKNOWN;
        try {
            //first search CDON to get an URL to the movie page        	
        	StringBuffer sb = new StringBuffer("http://cdon.se/search?q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8")); 
            
            String xml = webBrowser.request(sb.toString());
            //find url to movie in webpage
            int beginIndex = xml.indexOf("/section-movie.gif\" alt=\"\" />")+28;
            String movieURL = HTMLTools.extractTag(xml.substring(beginIndex), "<td class=\"title\">", 0);
            //sanity check on result before trying to load moviepage from url
            if(!movieURL.isEmpty()&& movieURL.contains("http")) {
            	//Split string to extract the url
            	String[] splitMovieURL = movieURL.split("\\s");
            	movieURL = splitMovieURL[1].replaceAll("href|=|\"", "");
            	logger.finest("found filmurl = " + movieURL);

            	//fetch movie page from cdon
            	StringBuffer buf = new StringBuffer(movieURL);
            	xml = webBrowser.request(buf.toString());
            	
            	//check if there is an image for this movie
            	//links to decent images har link text "Storre framsida"
            	if(xml.indexOf("St&#246;rre framsida") != -1) {
            		String[] htmlArray = xml.split("<");
            		//all cdon pages don't look the same so
            		//loop over the array to find the string with a link to an image
            		String[] posterURL = null;
            		for(String s : htmlArray) {
            			//found it!
            			if(s.indexOf("St&#246;rre framsida") != -1) {
            				posterURL = htmlArray[381].split("\"|\\s");
            				break;
            			}
            		}            	
            		//sanity check again (the found url should point to a jpg
            		if(posterURL[2].indexOf(".jpg") != -1 ) {
            			cdonPosterURL = "http://www.cdon.se/" + posterURL[2];
                		logger.finest("posterURL = " + cdonPosterURL);	
            		}
            		else {
            			logger.info("Should have found CDON poster for: " + movieName + "but didn\'t");
            		}
            		
            	}
            	//didn't find a page containing the text "storre framsida"
            	else {
            		logger.finer("No CDON image found for movie: " + movieName);
            	}
            }
            //search didn't even find an url to the movie
            else { 
            	logger.finer("Error in fetching movie from CDON for movie: " + movieName);
            }
            //finally return the image url
            return cdonPosterURL;
        } catch (Exception e) {
            logger.severe("Error while retreiving CDON image for movie : " + movieName);
            //logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
	}
}
