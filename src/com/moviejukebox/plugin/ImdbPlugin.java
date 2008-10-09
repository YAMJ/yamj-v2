package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.HTMLTools;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

public class ImdbPlugin implements MovieDatabasePlugin {

	public static String IMDB_PLUGIN_ID = "imdb";

	protected static Logger logger = Logger.getLogger("moviejukebox");
	protected String preferredSearchEngine;
	protected String preferredPosterSearchEngine;
	protected boolean perfectMatch;
	protected int maxGenres;
	protected String preferredCountry;
	protected String imdbPlot;

	public void init(Properties props) {
		preferredSearchEngine = props.getProperty("imdb.id.search", "imdb");
		preferredPosterSearchEngine = props.getProperty("imdb.alternate.poster.search", "google");
		perfectMatch = Boolean.parseBoolean(props.getProperty("imdb.perfect.match", "true"));
		preferredCountry = props.getProperty("imdb.preferredCountry", "USA");
		imdbPlot = props.getProperty("imdb.plot", "short");
		try {
			String temp = props.getProperty("imdb.genres.max", "9");
			System.out.println("imdb.genres.max=" + temp);
			maxGenres = Integer.parseInt(temp);
		} catch (NumberFormatException ex) {
			maxGenres = 9;
		}
	}

	public void scan(Movie mediaFile) {

		String imdbId = mediaFile.getId(IMDB_PLUGIN_ID);
		if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
			imdbId = getImdbId(mediaFile.getTitle(), mediaFile.getYear());
			mediaFile.setId(IMDB_PLUGIN_ID, imdbId);
		}

		if (!imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
			updateImdbMediaInfo(mediaFile);
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year. This routine is base on a IMDb request.
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
	 * retrieve the imdb matching the specified movie name and year. This routine is base on a yahoo request.
	 */
	private String getImdbIdFromYahoo(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://fr.search.yahoo.com/search;_ylt=A1f4cfvx9C1I1qQAACVjAQx.?p=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("+%28").append(year).append("%29");
			}

			sb.append("+site%3Aimdb.com&fr=yfp-t-501&ei=UTF-8&rd=r1");

			String xml = request(new URL(sb.toString()));
			int beginIndex = xml.indexOf("/title/tt");
			StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 7), "/\"");
			String imdbId = st.nextToken();

			if (imdbId.startsWith("tt")) {
				return imdbId;
			} else {
				return Movie.UNKNOWN;
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb Id for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year. This routine is base on a google request.
	 */
	private String getImdbIdFromGoogle(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.google.fr/search?hl=fr&q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("+%28").append(year).append("%29");
			}

			sb.append("+site%3Awww.imdb.com&meta=");

			String xml = request(new URL(sb.toString()));
			int beginIndex = xml.indexOf("/title/tt");
			StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 7), "/\"");
			String imdbId = st.nextToken();

			if (imdbId.startsWith("tt")) {
				return imdbId;
			} else {
				return Movie.UNKNOWN;
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb Id for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year. This routine is base on a IMDb request.
	 */
	private String getImdbIdFromImdb(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.imdb.com/find?s=tt&q=");
			sb.append(URLEncoder.encode(movieName, "iso-8859-1"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("+%28").append(year).append("%29");
			}
			sb.append(";s=tt;site=aka");

			String xml = request(new URL(sb.toString()));

			// Try to have a more accurate search result
			// by considering "exact matches" categories
			if (perfectMatch) {
				int beginIndex = xml.indexOf("Popular Titles");
				if (beginIndex != -1) {
					xml = xml.substring(beginIndex);
				}

				// Try to find an exact match first...
				// never know... that could be ok...
				int movieIndex;
				if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
					movieIndex = xml.indexOf(movieName + " </a> (" + year + ")");
				} else {
					movieIndex = xml.indexOf(movieName);
				}

				// Let's consider Exact Matches first
				beginIndex = xml.indexOf("Titles (Exact Matches)");
				if (beginIndex != -1 && movieIndex > beginIndex) {
					xml = xml.substring(beginIndex);
				}
			}

			int beginIndex = xml.indexOf("title/tt");
			StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 6), "/\"");
			String imdbId = st.nextToken();

			if (imdbId.startsWith("tt")) {
				return imdbId;
			} else {
				return Movie.UNKNOWN;
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb Id for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
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
			if (country == null) {
				if (value == null) {
					value = text;
				}
			} else {
				if (country.equals(preferredCountry)) {
					return text;
				}
			}
		}
		return value;
	}

	/**
	 * Scan IMDB html page for the specified movie
	 */
	private void updateImdbMediaInfo(Movie movie) {
		try {
			String xml = request(new URL("http://www.imdb.com/title/" + movie.getId(IMDB_PLUGIN_ID)));

			movie.setTitleSort(HTMLTools.extractTag(xml, "<title>", 0, "()><"));
			movie.setRating(parseRating(HTMLTools.extractTag(xml, "<div class=\"meta\">", 1)));
			// movie.setPlot(extractTag(xml, "<h5>Plot:</h5>"));
                        
			movie.setDirector(HTMLTools.extractTag(xml, "<h5>Director:</h5>", 1));
                        if (movie.getDirector() == null || movie.getDirector().isEmpty() || movie.getDirector().equalsIgnoreCase(Movie.UNKNOWN)) {
                            movie.setDirector(HTMLTools.extractTag(xml, "<h5>Directors:</h5>", 1));
                        }
                        
			movie.setReleaseDate(HTMLTools.extractTag(xml, "<h5>Release Date:</h5>"));
			movie.setRuntime(HTMLTools.extractTag(xml, "<h5>Runtime:</h5>"));

			movie.setCountry(HTMLTools.extractTag(xml, "<h5>Country:</h5>", 1));
			movie.setCompany(HTMLTools.extractTag(xml, "<h5>Company:</h5>", 1));
			int count = 0;
			for (String genre : HTMLTools.extractTags(xml, "<h5>Genre:</h5>", "</div>", "<a href=\"/Sections/Genres/", "</a>")) {
				movie.addGenre(genre);
				if (++count >= maxGenres) {
					break;
				}
			}

			String plot = "None";
			if (imdbPlot.equalsIgnoreCase("long")) {
				plot = getLongPlot(movie);
			}
			// even if "long" is set we will default to the "short" one if none
			// was found
			if (imdbPlot.equalsIgnoreCase("short") || plot.equals("None")) {
				plot = HTMLTools.extractTag(xml, "<h5>Plot:</h5>", 0, "><|");
				if (plot.startsWith("a class=\"tn15more")) {
					plot = "None";
				}
			}
			movie.setPlot(plot);

			movie.setCertification(getPreferredValue(HTMLTools.extractTags(xml, "<h5>Certification:</h5>", "</div>",
					"<a href=\"/List?certificates=", "</a>")));
                        if (movie.getCertification() == null || movie.getCertification().equalsIgnoreCase(Movie.UNKNOWN)) {
                            movie.setCertification(Movie.NOTRATED);
                        }

			if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                                movie.setYear(HTMLTools.extractTag(xml, "<a href=\"/Sections/Years/", 1));
                                if (movie.getYear() == null || movie.getYear().isEmpty() || movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
                                    movie.setYear(HTMLTools.extractTag(xml, "<h5>Original Air Date:</h5>", 2, " "));
                                }
			}

			movie.setCast(HTMLTools.extractTags(xml, "<table class=\"cast\">", "</table>", "<td class=\"nm\"><a href=\"/name/",
					"</a>"));

			if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
				movie.setPosterURL(getPosterURL(movie, xml));
			}

			if (movie.isTVShow()) {
				updateTVShowInfo(movie);
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb rating for movie : " + movie.getId(IMDB_PLUGIN_ID));
			e.printStackTrace();
		}
	}

	private int parseRating(String rating) {
		StringTokenizer st = new StringTokenizer(rating, "/ ()");
		try {
			return (int)(Float.parseFloat(st.nextToken()) * 10);
		} catch (Exception e) {
			return -1;
		}
	}

	protected String getPosterURL(Movie movie, String xml) {
		int castIndex = xml.indexOf("<h3>Cast</h3>");
		int beginIndex = xml.indexOf("src=\"http://ia.media-imdb.com/images");

		String posterURL = Movie.UNKNOWN;

		// Check posters.motechnet.com
		if (this.testMotechnetPoster(movie.getId(IMDB_PLUGIN_ID))) {
			posterURL = "http://posters.motechnet.com/covers/" + movie.getId(IMDB_PLUGIN_ID) + "_largeCover.jpg";
		} // Check www.impawards.com
		else if (!(posterURL = this.testImpawardsPoster(movie.getId(IMDB_PLUGIN_ID))).equals(Movie.UNKNOWN)) {
			// Cover Found
		} // Check www.moviecovers.com (if set in property file)
		else if ("moviecovers".equals(preferredPosterSearchEngine)
				&& !(posterURL = this.getPosterURLFromMoviecoversViaGoogle(movie.getTitle())).equals(Movie.UNKNOWN)) {
			// Cover Found
		} else if (beginIndex < castIndex && beginIndex != -1) {

			StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 5), "\"");
			posterURL = st.nextToken();
			int index = posterURL.indexOf("_SY");
			if (index != -1) {
				posterURL = posterURL.substring(0, index) + "_SY800_SX600_.jpg";
			}
		} else {
			// try searching an alternate search engine
			String alternateURL = Movie.UNKNOWN;
			if ("google".equalsIgnoreCase(preferredPosterSearchEngine)) {
				alternateURL = getPosterURLFromGoogle(movie.getTitle());
			} else if ("yahoo".equalsIgnoreCase(preferredPosterSearchEngine)) {
				alternateURL = getPosterURLFromYahoo(movie.getTitle());
			}

			if (!alternateURL.equalsIgnoreCase(Movie.UNKNOWN)) {
				posterURL = alternateURL;
			}
		}
		return posterURL;
	}

	/**
	 * retrieve the imdb matching the specified movie name and year. This routine is base on a yahoo request.
	 */
	protected String getPosterURLFromYahoo(String movieName) {
		try {
			StringBuffer sb = new StringBuffer("http://fr.images.search.yahoo.com/search/images?p=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));
			sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

			String xml = request(new URL(sb.toString()));
			int beginIndex = xml.indexOf("imgurl=");
			int endIndex = xml.indexOf("%26", beginIndex);

			if (beginIndex != -1 && endIndex > beginIndex) {
				return URLDecoder.decode(xml.substring(beginIndex + 7, endIndex), "UTF-8");
			} else {
				return Movie.UNKNOWN;
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving poster URL from yahoo images : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year. This routine is base on a yahoo request.
	 */
	protected String getPosterURLFromGoogle(String movieName) {
		try {
			StringBuffer sb = new StringBuffer("http://images.google.fr/images?q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));
			sb.append("&gbv=2");

			String xml = request(new URL(sb.toString()));
			int beginIndex = xml.indexOf("imgurl=") + 7;

			if (beginIndex != -1) {
				StringTokenizer st = new StringTokenizer(xml.substring(beginIndex), "\"&");
				return st.nextToken();
			} else {
				return Movie.UNKNOWN;
			}
		} catch (Exception e) {
			logger.severe("Failed retreiving poster URL from yahoo images : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	public boolean testMotechnetPoster(String movieId) {
		String content = null;
		try {
			content = request((new URL("http://posters.motechnet.com/title/" + movieId + "/")));
		} catch (Exception e) {
		}

		return content != null && content.contains("/covers/" + movieId + "_largeCover.jpg");
	}

	public String testImpawardsPoster(String movieId) {
		String returnString = Movie.UNKNOWN;
		String content = null;
		try {
			content = request((new URL(
					"http://search.yahoo.com/search?fr=yfp-t-501&ei=UTF-8&rd=r1&p=site:impawards.com+link:http://www.imdb.com/title/"
							+ movieId)));
		} catch (Exception e) {
		}

		if (content != null) {
			int indexMovieLink = content.indexOf("<span class=url>www.<b>impawards.com</b>/");
			if (indexMovieLink != -1) {
				String finMovieUrl = content.substring(indexMovieLink + 41, content.indexOf("</span>", indexMovieLink));
				finMovieUrl = finMovieUrl.replaceAll("<wbr />", "");

				int indexLastRep = finMovieUrl.lastIndexOf('/');
				String imgRepUrl = "http://www.impawards.com/" + finMovieUrl.substring(0, indexLastRep) + "/posters";
				returnString = imgRepUrl + finMovieUrl.substring(indexLastRep, finMovieUrl.lastIndexOf('.')) + ".jpg";
			}
		}

		return returnString;
	}

	protected String getPosterURLFromMoviecoversViaGoogle(String movieName) {
		try {
			String returnString = Movie.UNKNOWN;
			StringBuffer sb = new StringBuffer("http://www.google.com/search?meta=&q=site%3Amoviecovers.com+");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			String content = request(new URL(sb.toString()));
			if (content != null) {
				int indexMovieLink = content.indexOf("<a href=\"http://www.moviecovers.com/film/titre_");
				if (indexMovieLink != -1) {
					String finMovieUrl = content.substring(indexMovieLink + 47, content.indexOf("\" class=l>",
							indexMovieLink));
					returnString = "http://www.moviecovers.com/getjpg.html/"
							+ finMovieUrl.substring(0, finMovieUrl.lastIndexOf('.')).replace("+", "%20") + ".jpg";
				}
			}
			// System.out.println(returnString);
			return returnString;

		} catch (Exception e) {
			logger.severe("Failed retreiving moviecovers poster URL from google : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	public void scanTVShowTitles(Movie movie) {
		String imdbId = movie.getId(IMDB_PLUGIN_ID);
		if (!movie.isTVShow() || !movie.hasNewMovieFiles() ||
				imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
			return;
		}

		try {
			String xml = request(new URL("http://www.imdb.com/title/" + imdbId + "/episodes"));
			int season = movie.getSeason();
			for (MovieFile file : movie.getMovieFiles()) {
				if (!file.isNewFile()) {
					// don't scan episode title if it exists in XML data
					continue;
				}
				int episode = file.getPart();
				String episodeName = HTMLTools.extractTag(xml, "Season " + season + ", Episode " + episode + ":", 2);

				if (!episodeName.equals(Movie.UNKNOWN) && episodeName.indexOf("Episode #") == -1) {
					file.setTitle(episodeName);
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
		String plot = "None";

		try {
			String xml = request(new URL("http://www.imdb.com/title/" + movie.getId(IMDB_PLUGIN_ID) + "/plotsummary"));

			String result = HTMLTools.extractTag(xml, "<p class=\"plotpar\">");
			if (!result.equalsIgnoreCase(Movie.UNKNOWN) && result.indexOf("This plot synopsis is empty") < 0) {
				plot = result;
			}
		} catch (Exception e) {
			plot = "None";
		}

		return plot;
	}

	public void scanNFO(String nfo, Movie movie) {
		logger.finest("Scanning NFO for Imdb Id");
		int beginIndex = nfo.indexOf("/tt");
		if (beginIndex != -1) {
                    StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1),
                        "/ \n,:!&é\"'(--è_çà)=$");
                    movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
                    logger.finer("Imdb Id found in nfo = "+ movie.getId(ImdbPlugin.IMDB_PLUGIN_ID));
		} else {
                    beginIndex = nfo.indexOf("/Title?");
                    if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                        StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7),
                            "/ \n,:!&é\"'(--è_çà)=$");
                        movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, "tt" + st.nextToken());
                    } else {
			logger.finer("No Imdb Id found in nfo !");
                    }
		}
	}

	protected String request(URL url) throws IOException {
		return HTMLTools.request(url);
	}
}
