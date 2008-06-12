package com.moviejukebox.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.MovieJukeboxTools;
import com.moviejukebox.model.Movie;

public class ImdbPlugin implements MovieDatabasePlugin {
	
	private static Logger logger = Logger.getLogger("moviejukebox");
	
	private String preferredSearchEngine;
	private String preferredPosterSearchEngine;
	private boolean perfectMatch;

	public void scan(Movie mediaFile) {

		String imdbId = mediaFile.getId();
		if (imdbId == null || imdbId.equalsIgnoreCase("Unknown")) {
			imdbId = getImdbId(mediaFile.getTitle(), mediaFile.getYear());
			mediaFile.setId(imdbId);
		}

		if (!imdbId.equalsIgnoreCase("Unknown")) {
			updateImdbMediaInfo(mediaFile);
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year.
	 * This routine is base on a IMDb request.
	 */
	private String getImdbId(String movieName, String year) {
		if ("google".equalsIgnoreCase(preferredSearchEngine)) {
			return getImdbIdFromGoogle(movieName, year);
		} else if ("yahoo".equalsIgnoreCase(preferredSearchEngine)) {
			return getImdbIdFromYahoo(movieName, year);
		} else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
			return "Unknown";
		} else {
			return getImdbIdFromImdb(movieName, year);
		}
	}
	
	/**
	 * retrieve the imdb matching the specified movie name and year.
	 * This routine is base on a yahoo request.
	 */
	private String getImdbIdFromYahoo(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://fr.search.yahoo.com/search;_ylt=A1f4cfvx9C1I1qQAACVjAQx.?p=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));
			
			if (year != null && !year.equalsIgnoreCase("Unknown")) {
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
				return "Unknown";
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb Id for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return "Unknown";
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year.
	 * This routine is base on a google request.
	 */
	private String getImdbIdFromGoogle(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.google.fr/search?hl=fr&q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));
			
			if (year != null && !year.equalsIgnoreCase("Unknown")) {
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
				return "Unknown";
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb Id for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return "Unknown";
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year.
	 * This routine is base on a IMDb request.
	 */
	private String getImdbIdFromImdb(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.imdb.com/find?s=tt&q=");
			sb.append(URLEncoder.encode(movieName, "iso-8859-1"));
			
			if (year != null && !year.equalsIgnoreCase("Unknown")) {
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
				if (year != null && !year.equalsIgnoreCase("Unknown")) {
					movieIndex = xml.indexOf(movieName +" </a> ("+year+")");
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
				return "Unknown";
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb Id for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return "Unknown";
		}
	}

	/**
	 * Scan IMDB html page for the specified movie
	 */
	private void updateImdbMediaInfo(Movie movie) {
		try {
			String xml = request(new URL("http://www.imdb.com/title/" + movie.getId()));

			movie.setTitleSort(extractTag(xml, "<title>", 0, "()><"));
			movie.setRating(parseRating(extractTag(xml, "<b>User Rating:</b>",2)));
			movie.setPlot(extractTag(xml, "<h5>Plot:</h5>"));
			movie.setDirector(extractTag(xml, "<h5>Director:</h5>", 1));
			movie.setReleaseDate(extractTag(xml, "<h5>Release Date:</h5>"));
			movie.setRuntime(extractTag(xml, "<h5>Runtime:</h5>"));
			movie.setCountry(extractTag(xml, "<h5>Country:</h5>", 1));
			movie.setCompany(extractTag(xml, "<h5>Company:</h5>", 1));
			movie.addGenre(extractTag(xml, "<h5>Genre:</h5>", 1));
			movie.setPlot(extractTag(xml, "<h5>Plot:</h5>"));

			if (movie.getPlot().startsWith("a class=\"tn15more")) {
				movie.setPlot("None");
			}

			if (movie.getYear() == null 
			 || movie.getYear().isEmpty()
			 || movie.getYear().equalsIgnoreCase("Unknown")) {
			
				int beginIndex = xml.indexOf("<a href=\"/Sections/Years/");
				StringTokenizer st = new StringTokenizer(xml.substring(beginIndex + 25), "\"");
				try {
					movie.setYear(st.nextToken().trim());
				} catch (NumberFormatException e) { }
			}

			
			int castIndex = xml.indexOf("<h3>Cast</h3>");
			int beginIndex = xml.indexOf("src=\"http://ia.media-imdb.com/images");
			
			String posterURL = "Unknown";
			
			//Check posters.motechnet.com 
			if (this.testMotechnetPoster(movie.getId())) {
				posterURL = "http://posters.motechnet.com/covers/" + movie.getId() + "_largeCover.jpg";
			}
			//Check www.impawards.com 
			else if (!((posterURL =this.testImpawardsPoster(movie.getId())).equals("Unknown"))) {
				//Cover Found
			}
			//Check www.moviecovers.com (if set in property file)
			else if ( ("moviecovers".equals(preferredPosterSearchEngine))
					&&
					  !((posterURL =this.getPosterURLFromMoviecoversViaGoogle(movie.getTitle())).equals("Unknown"))
					) {
				//Cover Found
			}
			else if (beginIndex<castIndex && beginIndex != -1) {
				
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
			
			movie.setPosterURL(posterURL);

		} catch (Exception e) {
			logger.severe("Failed retreiving imdb rating for movie : " + movie.getId());
			e.printStackTrace();
		}
	}

	private int parseRating(String rating) {
		StringTokenizer st = new StringTokenizer(rating, "/ ()");
		try {
			return (int) Float.parseFloat(st.nextToken()) * 10;
		} catch(Exception e) {
			return -1;
		}
	}

	private String extractTag(String src, String findStr) {
		return this.extractTag(src, findStr, 0);
	}

	private String extractTag(String src, String findStr, int skip) {
		return this.extractTag(src, findStr, skip, "><");
	}

	private String extractTag(String src, String findStr, int skip,
			String separator) {
		int beginIndex = src.indexOf(findStr);
		StringTokenizer st = new StringTokenizer(src.substring(beginIndex
				+ findStr.length()), separator);
		for (int i = 0; i < skip; i++)
			st.nextToken();
		
		String value = MovieJukeboxTools.decodeHtml(st.nextToken().trim());
		if (   (value.indexOf("uiv=\"content-ty")!= -1) 
			    || (value.indexOf("cast")!= -1)
		    	|| (value.indexOf("title")!= -1)
			    || (value.indexOf("<")!= -1)) {
			value = "Unknown";
		}
		
		return value;
	}

	public String request(URL url) throws IOException {
		StringWriter content = null;

		try {
			content = new StringWriter();

			BufferedReader in = null;
			try {
				URLConnection cnx = url.openConnection();
				cnx.setRequestProperty(
					"User-Agent",
					"Mozilla/5.25 Netscape/5.0 (Windows; I; Win95)");

				in = new BufferedReader(new InputStreamReader(cnx.getInputStream()));

				String line;
				while ((line = in.readLine()) != null) {
					content.write(line);
				}
				
			} finally {
				if (in != null)
					in.close();
			}

			return content.toString();
		} finally {
			if (content != null)
				content.close();
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year.
	 * This routine is base on a yahoo request.
	 */
	private String getPosterURLFromYahoo(String movieName) {
		try {
			StringBuffer sb = new StringBuffer("	http://fr.images.search.yahoo.com/search/images?p=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));			
			sb.append("+poster&fr=&ei=utf-8&js=1&x=wrt");

			String xml = request(new URL(sb.toString()));
			int beginIndex = xml.indexOf("imgurl=");
			int endIndex = xml.indexOf("%26",beginIndex);

			if (beginIndex != -1 && endIndex>beginIndex) {
				return URLDecoder.decode(xml.substring(beginIndex + 7, endIndex), "UTF-8");
			} else {
				return "Unknown";
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving poster URL from yahoo images : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return "Unknown";
		}
	}

	/**
	 * retrieve the imdb matching the specified movie name and year.
	 * This routine is base on a yahoo request.
	 */
	private String getPosterURLFromGoogle(String movieName) {
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
				return "Unknown";
			}
		} catch (Exception e) {
			logger.severe("Failed retreiving poster URL from yahoo images : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return "Unknown";
		}
	}

	public boolean testMotechnetPoster(String movieId) throws IOException {
		String content = request((new URL("http://posters.motechnet.com/title/" + movieId + "/")));
		
		return ( (content!=null) && (content.contains("/covers/"+movieId+"_largeCover.jpg")));
		
	}


	public String testImpawardsPoster(String movieId) throws IOException {
		String returnString = "Unknown";
	    String content =request((new URL("http://search.yahoo.com/search?fr=yfp-t-501&ei=UTF-8&rd=r1&p=site:impawards.com+link:http://www.imdb.com/title/" + movieId)));
	    
		if (content!=null) {
	    int indexMovieLink = content.indexOf("<span class=url>www.<b>impawards.com</b>/");
		    if (indexMovieLink!=-1) {
		    	String finMovieUrl=content.substring(indexMovieLink+41, content.indexOf("</span>", indexMovieLink));
		    	finMovieUrl=finMovieUrl.replaceAll("<wbr />", "");
			    
		    	int indexLastRep = finMovieUrl.lastIndexOf('/');
		    	String imgRepUrl="http://www.impawards.com/"+finMovieUrl.substring(0, indexLastRep)+"/posters";
		    	returnString=imgRepUrl+finMovieUrl.substring(indexLastRep,finMovieUrl.lastIndexOf('.'))+".jpg";
		    }
		}

		return returnString;
	}

	private String getPosterURLFromMoviecoversViaGoogle(String movieName) {
		try {
			String returnString = "Unknown";
			StringBuffer sb = new StringBuffer("http://www.google.com/search?meta=&q=site%3Amoviecovers.com+");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));			

			String content = request(new URL(sb.toString()));
			if (content!=null) {
			    int indexMovieLink = content.indexOf("<a href=\"http://www.moviecovers.com/film/titre_");
			    if (indexMovieLink!=-1) {
			    	String finMovieUrl=content.substring(indexMovieLink+47, content.indexOf("\" class=l>", indexMovieLink));
			    	returnString="http://www.moviecovers.com/getjpg.html/"+finMovieUrl.substring(0,finMovieUrl.lastIndexOf('.')).replace("+","%20")+".jpg";
			    }
			}
//            System.out.println(returnString);
		    return returnString;

		} catch (Exception e) {
			logger.severe("Failed retreiving moviecovers poster URL from google : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return "Unknown";
		}
	}


	@Override
	public void init(Properties props) {
		preferredSearchEngine = props.getProperty("imdb.id.search", "imdb");
		preferredPosterSearchEngine = props.getProperty("imdb.alternate.poster.search", "google");
		perfectMatch = Boolean.parseBoolean(props.getProperty("imdb.perfect.match", "true"));
	}
}
