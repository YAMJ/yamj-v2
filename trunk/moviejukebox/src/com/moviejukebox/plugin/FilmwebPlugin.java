package com.moviejukebox.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class FilmwebPlugin extends ImdbPlugin {
	public static String FILMWEB_PLUGIN_ID = "filmweb";

	private static Logger logger = Logger.getLogger("moviejukebox");
	private static Pattern googlePattern = Pattern.compile("\"(http://[^\"/?&]*filmweb.pl[^\"]*)\"");
	private static Pattern yahooPattern = Pattern.compile("http%3a(//[^\"/?&]*filmweb.pl[^\"]*)\"");
	private static Pattern filmwebPattern = Pattern
			.compile("searchResultTitle[^>]+\"(http://[^\"/?&]*filmweb.pl[^\"]*)\"");
	private static Pattern nfoPattern = Pattern.compile("http://[^\"/?&]*filmweb.pl[^\\s<>`\"\\[\\]]*");
	private static Pattern longPlotUrlPattern = Pattern
			.compile("http://[^\"/?&]*filmweb.pl[^\"]*/opisy");
	private static Pattern posterUrlPattern = Pattern
			.compile("artshow[^>]+(http://gfx.filmweb.pl[^\"]+)\"");
	private static Pattern episodesUrlPattern = Pattern
			.compile("http://[^\"/?&]*filmweb.pl[^\"]*/odcinki");

	protected String filmwebPreferredSearchEngine;
	protected String filmwebPlot;

	public FilmwebPlugin() {
		super(); // use IMDB if filmweb doesn't know movie
		init();
	}

	public void init() {
		filmwebPreferredSearchEngine = PropertiesUtil.getProperty("filmweb.id.search", "filmweb");
		filmwebPlot = PropertiesUtil.getProperty("filmweb.plot", "short");
		try {
			// first request to filmweb site to skip welcome screen with ad banner
			webBrowser.request("http://www.filmweb.pl");
		} catch (IOException e) {
			logger.severe("Error : " + e.getMessage());
		}
	}

	public boolean scan(Movie mediaFile) {
		String filmwebUrl = mediaFile.getId(FILMWEB_PLUGIN_ID);
		if (filmwebUrl == null || filmwebUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
			filmwebUrl = getFilmwebUrl(mediaFile.getTitle(), mediaFile.getYear());
			mediaFile.setId(FILMWEB_PLUGIN_ID, filmwebUrl);
		}

                boolean retval = true;
		if (!filmwebUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
			retval = updateMediaInfo(mediaFile);
		} else {
			// use IMDB if filmweb doesn't know movie
			retval = super.scan(mediaFile);
		}
                return retval;
	}

	/**
	 * retrieve the filmweb url matching the specified movie name and year.
	 */
	protected String getFilmwebUrl(String movieName, String year) {
		if ("google".equalsIgnoreCase(preferredSearchEngine)) {
			return getFilmwebUrlFromGoogle(movieName, year);
		} else if ("yahoo".equalsIgnoreCase(preferredSearchEngine)) {
			return getFilmwebUrlFromYahoo(movieName, year);
		} else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
			return Movie.UNKNOWN;
		} else {
			return getFilmwebUrlFromFilmweb(movieName, year);
		}
	}

	/**
	 * retrieve the filmweb url matching the specified movie name and year. This routine is base on a yahoo request.
	 */
	private String getFilmwebUrlFromYahoo(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://search.yahoo.com/search?p=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("+%28").append(year).append("%29");
			}

			sb.append("+site%3Afilmweb.pl&ei=UTF-8");

			String xml = webBrowser.request(sb.toString());
			Matcher m = yahooPattern.matcher(xml);
			if (m.find()) {
				return "http:" + m.group(1);
			} else {
				return Movie.UNKNOWN;
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving filmweb url for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * retrieve the filmweb url matching the specified movie name and year. This routine is base on a google request.
	 */
	private String getFilmwebUrlFromGoogle(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.google.pl/search?hl=pl&q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("+%28").append(year).append("%29");
			}

			sb.append("+site%3Afilmweb.pl");

			String xml = webBrowser.request(sb.toString());
			Matcher m = googlePattern.matcher(xml);
			if (m.find()) {
				return m.group(1);
			} else {
				return Movie.UNKNOWN;
			}
		} catch (Exception e) {
			logger.severe("Failed retreiving filmweb url for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * retrieve the filmweb url matching the specified movie name and year. This routine is base on a filmweb request.
	 */
	private String getFilmwebUrlFromFilmweb(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.filmweb.pl/szukaj/film?q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("&startYear=").append(year).append("&endYear=").append(year);
			}
			String xml = webBrowser.request(sb.toString());
			Matcher m = filmwebPattern.matcher(xml);
			if (m.find()) {
				return m.group(1);
			} else {
				return Movie.UNKNOWN;
			}
		} catch (Exception e) {
			logger.severe("Failed retreiving filmweb url for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * Scan IMDB html page for the specified movie
	 */
	protected boolean updateMediaInfo(Movie movie) {
		try {
			String xml = webBrowser.request(movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));

                        if (xml.contains("Serial TV")) {
                            if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                                movie.setMovieType(Movie.TYPE_TVSHOW);
                                return false;
                            }
                        }
                        
                        if (!movie.isOverrideTitle()) {
                            movie.setTitle(HTMLTools.extractTag(xml, "<title>", 0, "()></"));
                        }
			movie.setRating(parseRating(HTMLTools.getTextAfterElem(xml, "film-rating-precise")));
			movie.setDirector(HTMLTools.getTextAfterElem(xml, "yseria"));
			movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "data premiery:"));
			movie.setRuntime(HTMLTools.getTextAfterElem(xml, "czas trwania:"));
			movie.setCountry(StringUtils.join(HTMLTools.extractTags(xml, "produkcja:", "gatunek", "<a ", "</a>"), ", "));

			int count = 0;
			String genres = HTMLTools.getTextAfterElem(xml, "gatunek:");
			if (!Movie.UNKNOWN.equals(genres)) {
				for (String genre : genres.split(" *, *")) {
					movie.addGenre(Library.getIndexingGenre(genre));
					if (++count >= maxGenres) {
						break;
					}
				}
			}

			String plot = "None";
			if (filmwebPlot.equalsIgnoreCase("long")) {
				plot = getLongPlot(xml);
			}
			// even if "long" is set we will default to the "short" one if none
			// was found
			if (filmwebPlot.equalsIgnoreCase("short") || plot.equals("None")) {
				plot = HTMLTools.getTextAfterElem(xml, "o-filmie-header", 1);
				if (plot.equals(Movie.UNKNOWN)) {
					plot = "None";
				}
			}
			movie.setPlot(plot);

			if (movie.getYear() == null || movie.getYear().isEmpty() ||
					movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
				movie.setYear(HTMLTools.extractTag(xml, "<title>", 1, "()><"));
			}

			movie.setCast(HTMLTools.extractTags(xml, "film-starring", "</table>", "<img ", "</a>"));

			if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
				movie.setPosterURL(getFilmwebPosterURL(movie, xml));
			}

			if (movie.isTVShow()) {
				updateTVShowInfo(movie, xml);
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving filmweb informations for movie : " + movie.getId(FilmwebPlugin.FILMWEB_PLUGIN_ID));
			e.printStackTrace();
		}
                return true;
	}

	private int parseRating(String rating) {
		try {
			return Math.round(Float.parseFloat(rating.replace(",", "."))) * 10;
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Retrieves the long plot description from filmweb if it exists, else "None"
	 *
	 * @return long plot
	 */
	private String getLongPlot(String mainXML) {
		String plot;
		try {
			// searchs for long plot url
			String longPlotUrl;
			Matcher m = longPlotUrlPattern.matcher(mainXML);
			if (m.find()) {
				longPlotUrl = m.group();
			} else {
				return "None";
			}
			String xml = webBrowser.request(longPlotUrl);
			plot = HTMLTools.getTextAfterElem(xml, "opisy-header", 2);
			if (plot.equalsIgnoreCase(Movie.UNKNOWN)) {
				plot = "None";
			}
		} catch (Exception e) {
			plot = "None";
		}

		return plot;
	}

	private String updateImdbId(Movie movie) {
		String imdbId = movie.getId(IMDB_PLUGIN_ID);
		if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
			imdbId = getImdbId(movie.getTitle(), movie.getYear());
			movie.setId(IMDB_PLUGIN_ID, imdbId);
		}
		return imdbId;
	}

	protected String getFilmwebPosterURL(Movie movie, String xml) {
		String posterURL = Movie.UNKNOWN;
		Matcher m = posterUrlPattern.matcher(xml);
		if (m.find()) {
			posterURL = m.group(1);
		} else {
			String imdbId = updateImdbId(movie);
			if (imdbId != null && !imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
				try {
					String imdbXml = webBrowser.request("http://www.imdb.com/title/" + imdbId);
					posterURL = super.getPosterURL(movie, imdbXml);
				} catch (IOException e) {
					return posterURL;
				}
			}
		}
		return posterURL;
	}

	public void scanTVShowTitles(Movie movie) {
		scanTVShowTitles(movie, null);
	}

	public void scanTVShowTitles(Movie movie, String mainXML) {
		if (!movie.isTVShow() || !movie.hasNewMovieFiles()) {
			return;
		}

		String filmwebUrl = movie.getId(FILMWEB_PLUGIN_ID);
		if (filmwebUrl == null || filmwebUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
			// use IMDB if filmweb doesn't know episodes titles
			super.scanTVShowTitles(movie);
			return;
		}

		try {
			if (mainXML == null) {
				mainXML = webBrowser.request(filmwebUrl);
			}
			// searchs for episodes url
			Matcher m = episodesUrlPattern.matcher(mainXML);
			if (m.find()) {
				String episodesUrl = m.group();
				String xml = webBrowser.request(episodesUrl);
				for (MovieFile file : movie.getMovieFiles()) {
					if (!file.isNewFile()) {
						// don't scan episode title if it exists in XML data
						continue;
					}
					int fromIndex = xml.indexOf("seria" + movie.getSeason());
					String episodeName = HTMLTools.getTextAfterElem(xml, "odcinek " + file.getPart(), 1, fromIndex);
					if (!episodeName.equals(Movie.UNKNOWN)) {
						file.setTitle(episodeName);
					}
				}
			} else {
				// use IMDB if filmweb doesn't know episodes titles
				updateImdbId(movie);
				super.scanTVShowTitles(movie);
			}
		} catch (IOException e) {
			logger.severe("Failed retreiving episodes titles for movie : " + movie.getTitle());
			logger.severe("Error : " + e.getMessage());
		}
	}

	protected void updateTVShowInfo(Movie movie, String mainXML) throws MalformedURLException, IOException {
		scanTVShowTitles(movie, mainXML);
	}

	public void scanNFO(String nfo, Movie movie) {
		super.scanNFO(nfo, movie); // use IMDB if filmweb doesn't know movie
		logger.finest("Scanning NFO for filmweb url");
		Matcher m = nfoPattern.matcher(nfo);
		boolean found = false;
		while (m.find()) {
			String url = m.group();
			if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") &&
			    !url.endsWith(".gif") && !url.endsWith(".png") && !url.endsWith(".bmp")) {
				found = true;
				movie.setId(FILMWEB_PLUGIN_ID, url);
			}
		}
		if (found) {
			logger.finer("Filmweb url found in nfo = " + movie.getId(FILMWEB_PLUGIN_ID));
		} else {
			logger.finer("No filmweb url found in nfo !");
		}
	}
}
