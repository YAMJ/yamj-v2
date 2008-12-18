package com.moviejukebox.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class SratimPlugin extends ImdbPlugin {
	public static String SRATIM_PLUGIN_ID = "sratim";

	private static Logger logger = Logger.getLogger("moviejukebox");
	private static Pattern googlePattern = Pattern.compile("\"(http://[^\"/?&]*sratim.co.il[^\"]*)\"");
	private static Pattern yahooPattern = Pattern.compile("http%3a(//[^\"/?&]*sratim.co.il[^\"]*)\"");
	private static Pattern sratimPattern = Pattern
			.compile("searchResultTitle[^>]+\"(http://[^\"/?&]*sratim.co.il[^\"]*)\"");
	private static Pattern nfoPattern = Pattern.compile("http://[^\"/?&]*sratim.co.il[^\\s<>`\"\\[\\]]*");
	private static Pattern longPlotUrlPattern = Pattern
			.compile("http://[^\"/?&]*sratim.co.il[^\"]*/opisy");
	private static Pattern posterUrlPattern = Pattern
			.compile("artshow[^>]+(http://www.sratim.co.il[^\"]+)\"");
	private static Pattern episodesUrlPattern = Pattern
			.compile("http://[^\"/?&]*sratim.co.il[^\"]*/odcinki");

	protected String sratimPreferredSearchEngine;
	protected String sratimPlot;

	public SratimPlugin() {
		super(); // use IMDB if sratim doesn't know movie
	}

	public boolean scan(Movie mediaFile) {

        String imdbId = mediaFile.getId(IMDB_PLUGIN_ID);
        if (imdbId == null || imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            imdbId = getImdbId(mediaFile.getTitle(), mediaFile.getYear());
            mediaFile.setId(IMDB_PLUGIN_ID, imdbId);
        }

        boolean retval = true;
        if (!imdbId.equalsIgnoreCase(Movie.UNKNOWN)) {
            retval = updateMediaInfo(mediaFile);
		} else {
			// use IMDB if sratim doesn't know movie
			retval = super.scan(mediaFile);
        }
        return retval;
	}

	/**
	 * retrieve the sratim url matching the specified movie name and year.
	 */
	protected String getSratimUrl(String movieName, String year) {
		if ("google".equalsIgnoreCase(preferredSearchEngine)) {
			return getSratimUrlFromGoogle(movieName, year);
		} else if ("yahoo".equalsIgnoreCase(preferredSearchEngine)) {
			return getSratimUrlFromYahoo(movieName, year);
		} else if ("none".equalsIgnoreCase(preferredSearchEngine)) {
			return Movie.UNKNOWN;
		} else {
			return getSratimUrlFromSratim(movieName, year);
		}
	}

	/**
	 * retrieve the sratim url matching the specified movie name and year. This routine is base on a yahoo request.
	 */
	private String getSratimUrlFromYahoo(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://search.yahoo.com/search?p=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("+%28").append(year).append("%29");
			}

			sb.append("+site%3Asratim.co.il&ei=UTF-8");

			String xml = webBrowser.request(sb.toString());
			Matcher m = yahooPattern.matcher(xml);
			if (m.find()) {
				return "http:" + m.group(1);
			} else {
				return Movie.UNKNOWN;
			}

		} catch (Exception e) {
			logger.severe("Failed retreiving sratim url for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * retrieve the sratim url matching the specified movie name and year. This routine is base on a google request.
	 */
	private String getSratimUrlFromGoogle(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.google.pl/search?hl=pl&q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("+%28").append(year).append("%29");
			}

			sb.append("+site%3Asratim.co.il");

			String xml = webBrowser.request(sb.toString());
			Matcher m = googlePattern.matcher(xml);
			if (m.find()) {
				return m.group(1);
			} else {
				return Movie.UNKNOWN;
			}
		} catch (Exception e) {
			logger.severe("Failed retreiving sratim url for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	/**
	 * retrieve the sratim url matching the specified movie name and year. This routine is base on a sratim request.
	 */
	private String getSratimUrlFromSratim(String movieName, String year) {
		try {
			StringBuffer sb = new StringBuffer("http://www.sratim.pl/szukaj/film?q=");
			sb.append(URLEncoder.encode(movieName, "UTF-8"));

			if (year != null && !year.equalsIgnoreCase(Movie.UNKNOWN)) {
				sb.append("&startYear=").append(year).append("&endYear=").append(year);
			}
			String xml = webBrowser.request(sb.toString());
			Matcher m = sratimPattern.matcher(xml);
			if (m.find()) {
				return m.group(1);
			} else {
				return Movie.UNKNOWN;
			}
		} catch (Exception e) {
			logger.severe("Failed retreiving sratim url for movie : " + movieName);
			logger.severe("Error : " + e.getMessage());
			return Movie.UNKNOWN;
		}
	}

	// Porting from my old code in c++
	public static final int BCT_L = 0;
	public static final int BCT_R = 1;
	public static final int BCT_N = 2;
	public static final int BCT_EN = 3;
	public static final int BCT_ES = 4;
	public static final int BCT_ET = 5;
	public static final int BCT_CS =6;

	// Return the type of a specific charcter
	private static int GetCharType(char C)
	{
		if (
			((C>='א') && (C<='ת'))
		   )
		   return BCT_R;

		if (
			(C==0x26) ||
			(C==0x40) ||
			((C>=0x41) && (C<=0x5A)) ||
			((C>=0x61) && (C<=0x7A)) ||
			((C>=0xC0) && (C<=0xD6)) ||
			((C>=0xD8) && (C<=0xDF))
		   )
		   return BCT_L;


		if (
			((C>=0x30) && (C<=0x39))
		   )
		   return BCT_EN;


		if (
			(C==0x2E) ||
			(C==0x2F)
		   )
		   return BCT_ES;


		if (
			(C==0x23) ||
			(C==0x24) ||
			((C>=0xA2) && (C<=0xA5)) ||
			(C==0x25) ||
			(C==0x2B) ||
			(C==0x2D) ||
			(C==0xB0) ||
			(C==0xB1)
		   )
		   return BCT_ET;


		if (
			(C==0x2C) ||
			(C==0x3A)
		   )
		   return BCT_CS;


		// Default Natural
		return BCT_N;
	}


	// Rotate a specific part of a string
	private static void RotateString(char[] String,int StartPos,int EndPos)
	{
		int Pos;
		char TempChar;

		for (Pos=0;Pos<(EndPos-StartPos+1)/2;Pos++)
		{
			TempChar=String[StartPos+Pos];
			
			String[StartPos+Pos]=String[EndPos-Pos];

			String[EndPos-Pos]=TempChar;
		}

	}



	// Set the string char types
	private static void SetStringCharType(char[] String,int[] CharType)
	{
		int Pos;

		Pos=0;

		while (Pos<String.length)
		{
			CharType[Pos]=GetCharType(String[Pos]);

			// Fix "(" and ")"
			if (String[Pos]==')')
				String[Pos]='(';
			else
			if (String[Pos]=='(')
				String[Pos]=')';

			Pos++;
		}

	}


	// Resolving Weak Types
	private static void ResolveWeakType(char[] String,int[] CharType)
	{
		int Pos;

		Pos=0;

		while (Pos<String.length)
		{
			// Check that we have at least 3 chars
			if (String.length-Pos>=3)
				if (
					(CharType[Pos]==BCT_EN) && 
					(CharType[Pos+2]==BCT_EN) &&
					( 
					  (CharType[Pos+1]==BCT_ES) ||
					  (CharType[Pos+1]==BCT_CS)
					)
				   )
					// Change the char type
					CharType[Pos+1]=BCT_EN;


			if (String.length-Pos>=2)
			{
				if (
					(CharType[Pos]==BCT_EN) && 
					(CharType[Pos+1]==BCT_ET)
				   )
					// Change the char type
					CharType[Pos+1]=BCT_EN;


				if (
					(CharType[Pos]==BCT_ET) && 
					(CharType[Pos+1]==BCT_EN)
				   )
					// Change the char type
					CharType[Pos]=BCT_EN;
			}


			// Default change all the terminators to natural
			if (
				(CharType[Pos]==BCT_ES) ||
				(CharType[Pos]==BCT_ET) ||
				(CharType[Pos]==BCT_CS)
			   )
				CharType[Pos]=BCT_N;

			Pos++;
		}

	/*
	- European Numbers (FOR ES,ET,CS)

		EN,ES,EN -> EN,EN,EN
		EN,CS,EN -> EN,EN,EN

		EN,ET -> EN,EN
		ET,EN -> EN,EN ->>>>> ET=EN


		else for ES,ET,CS (??)

			L,??,EN -> L,N,EN
	*/
	}


	// Resolving Natural Types
	private static void ResolveNaturalType(char[] String,int[] CharType,int DefaultDirection)
	{
		int Pos,CheckPos;
		int Before,After;

		Pos=0;

		while (Pos<String.length)
		{
			// Check if this is natural type and we need to cahnge it
			if (CharType[Pos]==BCT_N)
			{
				//Search for the type of the previous strong type
				CheckPos=Pos-1;

				while (true)
				{
					if (CheckPos<0)
					{
						// Default language
						Before=DefaultDirection;
						break;
					}

					if (CharType[CheckPos]==BCT_R)
					{
						Before=BCT_R;
						break;
					}

					if (CharType[CheckPos]==BCT_L)
					{
						Before=BCT_L;
						break;
					}

					CheckPos--;
				}


				CheckPos=Pos+1;

				//Search for the type of the next strong type
				while (true)
				{
					if (CheckPos>=String.length)
					{
						// Default language
						After=DefaultDirection;
						break;
					}

					if (CharType[CheckPos]==BCT_R)
					{
						After=BCT_R;
						break;
					}

					if (CharType[CheckPos]==BCT_L)
					{
						After=BCT_L;
						break;
					}

					CheckPos++;
				}


				// Change the natural depanded on the strong type before and after
				if ((Before==BCT_R) && (After==BCT_R))
					CharType[Pos]=BCT_R;
				else
					if ((Before==BCT_L) && (After==BCT_L))
						CharType[Pos]=BCT_L;
					else
						CharType[Pos]=DefaultDirection;
			}

			Pos++;
		}


	/*
		R N R -> R R R
		L N L -> L L L

		L N R -> L e R (e=default)
		R N L -> R e L (e=default)
	*/
	}



	// Resolving Implicit Levels
	private static void ResolveImplictLevels(char[] String,int[] CharType,int[] Level)
	{
		int Pos;

		Pos=0;

		while (Pos<String.length)
		{
			if (CharType[Pos]==BCT_L)
				Level[Pos]=2;


			if (CharType[Pos]==BCT_R)
				Level[Pos]=1;


			if (CharType[Pos]==BCT_EN)
			{
//				if (Pos==0)
					Level[Pos]=2;
/*					
				else
					// Check if the previous char
					if (CharType[Pos]==BCT_R)
						Level[Pos]=1;
					else
						Level[Pos]=0;
*/
			}

			Pos++;
		}
	}

	// Reordering Resolved Levels
	private static void ReorderResolvedLevels(char[] String,int[] Level)
	{
		int Count;
		int StartPos,EndPos,Pos;

		for (Count=2;Count>=1;Count--)
		{
			Pos=0;

			while (Pos<String.length)
			{
				// Check if this is the level start
				if (Level[Pos]>=Count)
				{
					StartPos=Pos;

					// Search for the end
					while ((Pos+1!=String.length) && (Level[Pos+1]>=Count))
						Pos++;

					EndPos=Pos;

					RotateString(String,StartPos,EndPos);
				}

				Pos++;
			}
		}
	}

	// Convert logical string to visual
	private static void LogicalToVisual(char[] String,int DefaultDirection)
	{
		int[] CharType;
		int[] Level;

		int Len;

		Len=String.length;

		// Allocate CharType and Level arrays
		CharType=new int[Len];

		Level=new int[Len];

		// Set the string char types
		SetStringCharType(String,CharType);

		// Resolving Weak Types
		ResolveWeakType(String,CharType);

		// Resolving Natural Types
		ResolveNaturalType(String,CharType,DefaultDirection);

		// Resolving Implicit Levels
		ResolveImplictLevels(String,CharType,Level);

		// Reordering Resolved Levels
		ReorderResolvedLevels(String,Level);
	}


	private static boolean isCharNatural( char c)
	{
		if (( c == ' ' ) ||
			( c == '-' ))
			return true;
			
		return false;
	}

	private static String logicalToVisual( String text)
	{
		char[] ret = new char[text.length()];
		
		ret=text.toCharArray();
		
		LogicalToVisual(ret,BCT_R);
		String s = new String(ret);
		return s;
	}

	private static ArrayList<String> logicalToVisual( ArrayList<String> text)
	{
		ArrayList<String> ret = new ArrayList<String>();

		for( int i=0; i < text.size(); i++ )
		{
			ret.add( logicalToVisual(text.get(i)) );

		}

		return ret;
	}

	private static String removeTrailDot( String text)
	{
		int dot = text.lastIndexOf(".");
		
		if (dot == -1)
			return text;

		return text.substring(0,dot);
	}

	private static String removeTrailBracket( String text)
	{
		int bracket = text.lastIndexOf(" (");
		
		if (bracket == -1)
			return text;

		return text.substring(0,bracket);
	}

	private static String breakLongLines( String text, int lineMaxChar, int lineMax)
	{
		String ret = new String();

		int scanPos = 0;
		int lastBreakPos = 0;
		int lineStart = 0;
		int lineCount = 0;
		
		while (scanPos < text.length())
		{
			if (isCharNatural( text.charAt(scanPos) ))
				lastBreakPos = scanPos;
			
			if ( scanPos-lineStart > lineMaxChar )
			{
				// Check if no break position found
				if (lastBreakPos == 0)
					// Hard break on this location
					lastBreakPos=scanPos;
				
				ret = ret + logicalToVisual(text.substring(lineStart,lastBreakPos).trim()) + "{br}";
				
				lineStart = lastBreakPos;
				lastBreakPos=0;
				lineCount++;
				
				if (lineCount==lineMax)
					return ret + "...";
			}
			
			scanPos++;
		}
		
		ret = ret + logicalToVisual(text.substring(lineStart,scanPos).trim());
		
		return ret;
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

	/**
	 * Scan IMDB html page for the specified movie
	 */
	protected boolean updateMediaInfo(Movie movie) {
		try {
			String xml = webBrowser.request("http://www.sratim.co.il/movies/search.aspx?Keyword=" + movie.getId(IMDB_PLUGIN_ID));

			String detailsUrl = HTMLTools.extractTag(xml, "cellpadding=\"0\" cellspacing=\"0\" onclick=\"document.location='", 0, "'");

			if (detailsUrl.equalsIgnoreCase(Movie.UNKNOWN))
				return false;


			xml = webBrowser.request("http://www.sratim.co.il/" + detailsUrl);
			

            if (detailsUrl.contains("series")) {
                if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {
                    movie.setMovieType(Movie.TYPE_TVSHOW);
                }
            }
            
            if (!movie.isOverrideTitle()) {
				String title=removeTrailBracket(HTMLTools.extractTag(xml, "<td valign=\"top\" style=\"width:100%\"", 0, "</td>"));
				
                movie.setTitle(logicalToVisual(title));
                movie.setTitleSort(title);
            }
                        
			movie.setRating(parseRating(HTMLTools.extractTag(xml, "<span style=\"font-size:12pt;font-weight:bold\"><img alt=\"", 0, "/")));
			movie.setDirector(logicalToVisual(HTMLTools.getTextAfterElem(xml, "במאי:")));
			movie.setReleaseDate(HTMLTools.getTextAfterElem(xml, "תאריך יציאה לקולנוע בחו\"ל:"));
			movie.setRuntime(logicalToVisual(removeTrailDot(HTMLTools.getTextAfterElem(xml, "אורך:"))));
			movie.setCountry(logicalToVisual(HTMLTools.getTextAfterElem(xml, "מדינה:")));

			int count = 0;
			String genres = HTMLTools.getTextAfterElem(xml, "ז'אנר:");
			if (!Movie.UNKNOWN.equals(genres)) {
				for (String genre : genres.split(" *, *")) {
					movie.addGenre(logicalToVisual(Library.getIndexingGenre(genre)));
					if (++count >= maxGenres) {
						break;
					}
				}
			}
			
            String tmpPlot = removeHtmlTags(extractTag(xml, "<b><u>תקציר:</u></b><br />", "</div>"));
			
			movie.setPlot(breakLongLines(tmpPlot,65,10));

			if (movie.getYear() == null || movie.getYear().isEmpty() ||
					movie.getYear().equalsIgnoreCase(Movie.UNKNOWN)) {
					
				
				if (detailsUrl.contains("series"))
					movie.setYear(HTMLTools.extractTag(xml, "<span style=\"font-weight:normal\">(", 0, ")"));
				else					
					movie.setYear(HTMLTools.getTextAfterElem(xml, "<span id=\"ctl00_ctl00_Body_Body_Box_ProductionYear\">"));
			}

			movie.setCast(logicalToVisual(HTMLTools.extractTags(xml, "שחקנים:", "<br />", "<a href", "</a>")));
			if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase(Movie.UNKNOWN)) {
				movie.setPosterURL( "http://www.sratim.co.il/movies/" + HTMLTools.extractTag(xml, "<img src=\"/movies/", 0, "\""));
			}

			if (movie.isTVShow()) {
				updateTVShowInfo(movie, xml);
			}


		} catch (Exception e) {
			logger.severe("Failed retreiving sratim informations for movie : " + movie.getId(SratimPlugin.SRATIM_PLUGIN_ID));
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
	 * Retrieves the long plot description from sratim if it exists, else "None"
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


	public void scanTVShowTitles(Movie movie) {
		scanTVShowTitles(movie, null);
	}

	public void scanTVShowTitles(Movie movie, String mainXML) {
		if (!movie.isTVShow() || !movie.hasNewMovieFiles()) {
			return;
		}


		if (mainXML == null) {
			// use IMDB if sratim doesn't know episodes titles
			super.scanTVShowTitles(movie);
			return;
		}


		for (MovieFile file : movie.getMovieFiles()) {
			if (!file.isNewFile()) {
				// don't scan episode title if it exists in XML data
				continue;
			}
			
			String episodeName = logicalToVisual(HTMLTools.getTextAfterElem(mainXML, "<b>פרק " + file.getPart() + "</b> - "));
			if (!episodeName.equals(Movie.UNKNOWN)) {
				file.setTitle(episodeName);
			}
		}

/*
		try {
			if (mainXML == null) {
				String startimUrl = movie.getId(SRATIM_PLUGIN_ID);
				if (sratimUrl == null || sratimUrl.equalsIgnoreCase(Movie.UNKNOWN)) {
					// use IMDB if sratim doesn't know episodes titles
					super.scanTVShowTitles(movie);
					return;
				}
					
				mainXML = webBrowser.request(sratimUrl);
				
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
				// use IMDB if sratim doesn't know episodes titles
				updateImdbId(movie);
				super.scanTVShowTitles(movie);
			}
		} catch (IOException e) {
			logger.severe("Failed retreiving episodes titles for movie : " + movie.getTitle());
			logger.severe("Error : " + e.getMessage());
		}
		*/
	}

	protected void updateTVShowInfo(Movie movie, String mainXML) throws MalformedURLException, IOException {
		scanTVShowTitles(movie, mainXML);
	}

	public void scanNFO(String nfo, Movie movie) {
		super.scanNFO(nfo, movie); // use IMDB if sratim doesn't know movie
		logger.finest("Scanning NFO for sratim url");
		Matcher m = nfoPattern.matcher(nfo);
		boolean found = false;
		while (m.find()) {
			String url = m.group();
			if (!url.endsWith(".jpg") && !url.endsWith(".jpeg") &&
			    !url.endsWith(".gif") && !url.endsWith(".png") && !url.endsWith(".bmp")) {
				found = true;
				movie.setId(SRATIM_PLUGIN_ID, url);
			}
		}
		if (found) {
			logger.finer("Sratim url found in nfo = " + movie.getId(SRATIM_PLUGIN_ID));
		} else {
			logger.finer("No sratim url found in nfo !");
		}
	}
}
