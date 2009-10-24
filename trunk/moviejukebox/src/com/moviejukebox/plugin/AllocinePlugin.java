/*
 *      Copyright (c) 2004-2009 YAMJ Members
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.PosterScanner;
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
				movie.setTitle(extractTag(xml, "<h1>", "</h1>"));
			}
			movie.setRating(parseRating(extractTag(xml, "<p class=\"withstars\">", "</p>")));
//			logger.finest("TV Show rating = " + movie.getRating());
			String tmpPlot = removeHtmlTags(extractTag(xml, "Synopsis :", "</p>"));
			// limit plot to ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT char
			if (tmpPlot.length() > ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT) {
				tmpPlot = tmpPlot.substring(0, Math.min(tmpPlot.length(), ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT)) + "...";
			}
			movie.setPlot(tmpPlot);
//			logger.finest("TV Show Plot = " + movie.getPlot());
			movie.setDirector(removeHtmlTags(extractTag(xml, "Créée par", "</span>")));
//			logger.finest("TV Show Director = " + movie.getDirector());

			//            movie.setRuntime(extractTag(xml, "Format</span> : ", "."));
			movie.setCountry(removeOpenedHtmlTags(extractTag(xml, "<a href='/film/tous/", "</a>")));
//			logger.finest("TV Show country = " + movie.getCountry());

			for (String genre : extractTags(xml, "Genre :", "</p>", "<a href='/series/toutes/genre", "</a>")) {
				movie.addGenre(Library.getIndexingGenre(removeOpenedHtmlTags(genre)));
			}

			// movie.setCertification(getPreferredValue(extractTags(xml,
			// "<h5>Certification:</h5>", "</div>",
			// "<a href=\"/List?certificates=", "</a>")));

			if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase("Unknown")) {
				movie.setYear(removeOpenedHtmlTags(extractTag(xml, "en <a href=\"/series/toutes/", "</a>")));
//				logger.finest("TV Show year = " + movie.getYear());
			}

			// Get Fanart
			if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
				movie.setFanartURL(getFanartURL(movie));
				if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
					movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
				}
			}

			if (movie.getCast().isEmpty()) {
				for (String actor : extractTags(xml, "Avec ", "<br />", "<a", "</a>")) {
//					logger.finest("actorTag=["+actor+"]");
					if (actor.contains("/personne/fichepersonne_gen_cpersonne")) {
					String cleanActor = removeOpenedHtmlTags(actor);
//						logger.finest("Actor added : " + cleanActor);
						movie.addActor(cleanActor);
					}
				}
			}

			// Allocine does not old posters for Tv Shows
			//            updatePoster(movie);
			String posterURL = PosterScanner.getPosterURL(movie, xml, IMDB_PLUGIN_ID);
			logger.finest("Movie PosterURL from other source : " + posterURL);
			movie.setPosterURL(posterURL);

			scanTVShowTitles(movie);
		} catch (Exception error) {
			logger.severe("Failed retreiving alloCine TVShow info for " + movie.getId(ALLOCINE_PLUGIN_ID));
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
//			logger.finest(
//					"Start a new request for seasons details : http://www.allocine.fr/seriespage_seasonepisodes_last?cseries=" + allocineId);
					
			String xml = webBrowser.request("http://www.allocine.fr/seriespage_seasonepisodes_last?cseries=" + allocineId);

			for (String seasonTag : extractHtmlTags(xml, "Saison :", "</ul>", "<a href=\"/seriespage_seasonepisodes_last", "</a>")) {
				try {
//					logger.finest("New Season detected seasonTag=[" + seasonTag+"]");
					String seasonString = removeHtmlTags(seasonTag);
//					logger.finest("New Season detected seasonString=[" + seasonString+"]");
					int seasonNbI = Integer.valueOf(seasonString);
//					logger.finest("New Season detected seasonNbI=[" + seasonNbI+"]");
					String seasonStringId = seasonTag.substring(seasonTag.lastIndexOf('=')+1, seasonTag.lastIndexOf('"'));
//					logger.finest("New Season detected seasonStringId=[" + seasonStringId+"]");
					int seasonIdI = Integer.valueOf(seasonStringId);
//					logger.finest("New Season detected seasonIdI= " + seasonIdI);
//					logger.finest("seasonNbI = " + seasonNbI + ", movie.getSeason() = " + movie.getSeason());
					if (seasonNbI == movie.getSeason()) {
						// we found the right season, time to get the infos
//						logger.finest("The right SeasonIdI = " + seasonIdI);
						xml = webBrowser.request("http://www.allocine.fr/seriespage_seasonepisodes_last?cseries="+allocineId+"&cseriesseason=" + seasonIdI);
						
						HashMap<Integer,String> episodeNames = new HashMap<Integer,String>();  
						for (String episode : extractTags(xml, "<table class=\"boxofficedata\">", "</table>", "<a", "</a>")) {
							try {
//								logger.finest("episodeTag=[" + episode+"]");
								String episodeNbS = episode.substring(episode.lastIndexOf("Episode")+8,episode.lastIndexOf(':') ).trim();
//								logger.finest("episodeNbS=[" + episodeNbS+"]");
								Integer episodeNbI =  new Integer(episodeNbS);
								String episodeName = episode.substring(episode.lastIndexOf(':')+1).trim();
//								logger.finest("episodeName=[" + episodeName+"]");
								episodeNames.put(episodeNbI,episodeName);
//								logger.finest("Episode Nb "+ episodeNbI + " : [" + episodeName+"] added to map");
							}catch (Exception e) {
								logger.severe("Error while parsing episode names : " +e.getMessage());
								// skip to next episode
							}
						}
						for (MovieFile file : movie.getFiles()) {
							if (!file.isNewFile() || file.hasTitle()) {
								// don't scan episode title if it exists in XML data
								continue;
							}
							logger.finest("Setting filename for episode Nb "+ file.getFirstPart());
							file.setTitle(episodeNames.get(file.getFirstPart()));
						}
					}
				} catch (Exception error) {
					// logger.severe("Error while getting season infos " + e);
					// nothing to do, we skip this season
				}
			}
		} catch (Exception error) {
			logger.severe("Failed retreiving episodes titles for movie : " + movie.getTitle());
			logger.severe("Error : " + error.getMessage());
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
				movie.setTitle(extractTag(xml, "<h1>", "</h1>"));
			}

			if (movie.getOriginalTitle().equalsIgnoreCase(Movie.UNKNOWN)){
				movie.setOriginalTitle(removeHtmlTags(extractTag(xml, "Titre original :", "</em>")));
			}

			if (movie.getRating() == -1){
				movie.setRating(parseRating(extractTag(xml, "Note Moyenne:", "</span>")));
//				logger.finest("Movie rating = " + movie.getRating());
			}

			if (movie.getPlot().equalsIgnoreCase(Movie.UNKNOWN)){
				// limit plot to ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT char
				String tmpPlot = removeHtmlTags(extractTag(xml, "<span class=\"bold\">Synopsis :</span>", "</p>")).trim();
				if (tmpPlot.length() > ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT) {
					tmpPlot = tmpPlot.substring(0, Math.min(tmpPlot.length(), ALLOCINE_PLUGIN_PLOT_LENGTH_LIMIT)) + "...";
				}
				movie.setPlot(tmpPlot);
//				logger.finest("Movie Plot = " + movie.getPlot());
			}

			if (movie.getDirector().equalsIgnoreCase(Movie.UNKNOWN)){
				movie.setDirector(removeHtmlTags(extractTag(xml, "Réalisé par ", "</span>")));
//				logger.finest("Movie Director = " + movie.getDirector());
			}

			if (movie.getReleaseDate().equals(Movie.UNKNOWN)) {
				movie.setReleaseDate(removeHtmlTags(extractTag(xml, "Date de sortie cinéma :", "</a>")).trim());
//				logger.finest("Movie Theater release date = [" + movie.getReleaseDate()+"]");
			}

			if (movie.getRuntime().equals(Movie.UNKNOWN)) {
				movie.setRuntime(extractTag(xml, "Durée : ", "."));
//				logger.finest("Durée = " + movie.getRuntime());
			}

			if (movie.getCountry().equals(Movie.UNKNOWN)) {
				movie.setCountry(removeHtmlTags(extractTag(xml, "Long-métrage", "</a>")).trim());
//				logger.finest("Movie Country = " + movie.getCountry());
			}

			//            if (movie.getCompany().equals(Movie.UNKNOWN)) {
			//                movie.setCompany(removeHtmlTags(extractTag(xml, "Distribué par ", "</h3>")));
			//                logger.finest("Movie Company = " + movie.getCompany());
			//            }

			if (movie.getGenres().isEmpty()) {
				for (String genre : extractTags(xml, "Genre : ", "</h3>", "/film/tous/genre-", "</a>")) {
					movie.addGenre(removeOpenedHtmlTags(genre));
				}
			}
			// movie.setCertification(getPreferredValue(extractTags(xml,
			// "<h5>Certification:</h5>", "</div>",
			// "<a href=\"/List?certificates=", "</a>")));

			if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
				movie.setYear(removeHtmlTags(extractTag(xml, "Année de production : ", "</a>")));
			}

			// Get Fanart
			if (downloadFanart && (movie.getFanartURL() == null || movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN))) {
				movie.setFanartURL(getFanartURL(movie));
				if (movie.getFanartURL() != null && !movie.getFanartURL().equalsIgnoreCase(Movie.UNKNOWN)) {
					movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
				}
			}

			if (movie.getCast().isEmpty()) {
				for (String actor : extractTags(xml, "Avec ", "<br />", "<a", "</a>")) {
//					logger.finest("actorTag=["+actor+"]");
					if (actor.contains("/personne/fichepersonne_gen_cpersonne")) {
					String cleanActor = removeOpenedHtmlTags(actor);
//						logger.finest("Actor added : " + cleanActor);
						movie.addActor(cleanActor);
					}
				}
			}
			String posterMediaIdTag = extractTag(xml,"<div class=\"poster\">"," alt=");
//			logger.finest("Allocine posterMediaIdTag : [" + posterMediaIdTag+"]");
			String posterMediaId = posterMediaIdTag.substring(posterMediaIdTag.lastIndexOf('/')+1,posterMediaIdTag.lastIndexOf('.'));
//			logger.finest("Allocine posterMediaId : [" + posterMediaId+"]");
			updatePoster(movie, posterMediaId);

		} catch (IOException error) {
			logger.severe("Failed retreiving allocine infos for movie : " + movie.getId(ALLOCINE_PLUGIN_ID));
			final Writer eResult = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(eResult);
			error.printStackTrace(printWriter);
			logger.severe(eResult.toString());
		}
		return true;
	}

	private void updatePoster(Movie movie, String posterMediaId) {
		String posterURL = Movie.UNKNOWN;
		String xml = "";

		// make an IMDb request for poster
		if (movie.getPosterURL() != null && !movie.getPosterURL().equalsIgnoreCase("Unknown")) {
			// we already have a poster URL
			logger.finer("Movie already has PosterURL : " + movie.getPosterURL());
			return;
		}

		try {
			// Check alloCine first only for movies because TV Show posters are
			// wrong.
			if (!movie.isTVShow()) {
				String mediaFileURL = "http://www.allocine.fr/film/fichefilm-"+ movie.getId(ALLOCINE_PLUGIN_ID) + "/affiches/detail/?cmediafile=" + posterMediaId;
//				logger.finest("Allocine mediaFileURL : " + mediaFileURL);
				xml = webBrowser.request(mediaFileURL);

				String posterURLTag = extractTag(xml, "<div class=\"tac\" style=\"\">", "</div>");
//				logger.finest("Allocine posterURLTag : " + posterURLTag);
				posterURL = extractTag(posterURLTag, "<img src=\"","\"");

				if (!posterURL.equalsIgnoreCase(Movie.UNKNOWN)) {
					logger.finest("Movie PosterURL from Allocine: " + posterURL);
					movie.setPosterURL(posterURL);
					return;
				}

				posterURL = PosterScanner.getPosterURL(movie, xml, IMDB_PLUGIN_ID);
				logger.finest("Movie PosterURL from other source : " + posterURL);
				movie.setPosterURL(posterURL);
				return;
			}
		} catch (Exception error) {
			logger.severe("Failed retreiving poster for movie : " + movie.getId(ALLOCINE_PLUGIN_ID));
			final Writer eResult = new StringWriter();
			final PrintWriter printWriter = new PrintWriter(eResult);
			error.printStackTrace(printWriter);
			logger.severe(eResult.toString());
		}
	}

	private int parseRating(String rating) {

		int index = rating.indexOf("(");
        if (index < 0) return -1;
        try {
            String floatRating= rating.substring(index+1, index + 4).replace(',','.');
            //logger.finest("String floatRating =["+ floatRating+"]");

            return (int)(Float.parseFloat(floatRating) / 4.0 * 100);
		} catch (Exception error) {
            logger.finest("Error parsing rating:"+rating);
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
		} catch (ParseException error) {
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
		String allocineBaseRequest = "http://www.allocine.fr/recherche/?q=";
		try {
			StringBuffer sb = new StringBuffer(allocineBaseRequest);
			sb.append(URLEncoder.encode(movieName, "UTF-8").replace(' ', '+'));
			logger.finest("Allocine request : "+sb.toString());
			String xml = webBrowser.request(sb.toString());

			String alloCineStartResult;
			String alloCineMediaPrefix;
			if (mediaFile.isTVShow()) {
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

			for (String searchResult : extractTags(xml, alloCineStartResult, alloCineEndResult, alloCineMediaPrefix, alloCineMediaSectionTagEnd)) {
				//                 logger.finest("AlloCine SearchResult = " + searchResult);
				String searchResultYear = searchResult.substring(searchResult.lastIndexOf(alloCineYearTagStart) + alloCineYearTagStart.length(), searchResult.length());
				searchResultYear = searchResultYear.substring(0,searchResultYear.indexOf(alloCineYearTagEnd)).trim();
				//                 logger.finest("AlloCine searchResultYear = [" + searchResultYear+"] while year=["+year+"]");
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
		} catch (Exception error) {
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
		} catch (Exception error) {
			logger.severe("extractTag an exception occurred during tag extraction : " + error);
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
		result = result.trim();
		//         logger.finest("removeOpenedHtmlTags before=[" + src + "], after=["+ result + "]");
		return result;
	}

	protected ArrayList<String> extractTags(String src, String sectionStart, String sectionEnd, String startTag, String endTag) {
		ArrayList<String> tags = new ArrayList<String>();
		int index = src.indexOf(sectionStart);
		if (index == -1) {
			logger.finest("extractTags no sectionStart Tags found");
			return tags;
		}
		index += sectionStart.length();
		int endIndex = src.indexOf(sectionEnd, index);
		if (endIndex == -1) {
			logger.finest("extractTags no sectionEnd Tags found");
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
		//         logger.finest("extractTags sectionText = " + sectionText);
		//         logger.finest("extractTags startTag = " + startTag);
		//         logger.finest("extractTags startTag index = " + index);
		while (index != -1) {
			index += startLen;
			endIndex = sectionText.indexOf(endTag, index);
			if (endIndex == -1) {
				logger.finest("extractTags no endTag found");
				endIndex = lastIndex;
			}
			String text = sectionText.substring(index, endIndex);
			//             logger.finest("extractTags Tag found text = [" + text+"]");

			// replaceAll used because trim() does not trim unicode space
			tags.add(HTMLTools.decodeHtml(text.trim()));
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
