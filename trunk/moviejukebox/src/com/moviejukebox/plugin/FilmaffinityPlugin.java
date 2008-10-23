package com.moviejukebox.plugin;

import java.net.URLEncoder;
import java.util.StringTokenizer;


import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

public class FilmaffinityPlugin extends ImdbPlugin
{

  public static String FILMAFFINITY_PLUGIN_ID = "filmaffinity";
  //Define plot length
  int preferredPlotLength = Integer.parseInt(PropertiesUtil.getProperty( "filmaffinity.plot.length", "500" ));

  public FilmaffinityPlugin()
  {
    super();
    preferredCountry = PropertiesUtil.getProperty( "imdb.preferredCountry", "Spain" );
    
  }
 @Override
  public void scan( Movie mediaFile )
  {    
  	String imdbId = mediaFile.getId( IMDB_PLUGIN_ID );     
  	
    // Spanish's title and plot
    String titleSpanish = "None";
    String plotSpanish = "None";   
    
    if ( imdbId == null || imdbId.equalsIgnoreCase( Movie.UNKNOWN ) )
    {
    		// Save original title
        titleSpanish = mediaFile.getTitle();
        
    		String filmAffinityId = getFilmAffinityId( mediaFile.getTitle(), mediaFile.getYear(), mediaFile.getSeason() );
    		if (filmAffinityId.indexOf(".html") != -1)
    			// Update original title and plot (in Spanish)
    			updateFilmAffinityMediaInfo ( mediaFile, filmAffinityId );
    		
    		// Save plot in Spanish    	   	
        plotSpanish = mediaFile.getPlot();    	
    }
    
    // Other info from imdb
    super.scan(mediaFile);
    
    // Update plot and title in Spanish   
    if (plotSpanish != "None" && plotSpanish != "UNKNOWN")
    	mediaFile.setPlot(plotSpanish);
    mediaFile.setTitle(titleSpanish);   
  }
  
  /**
  * retrieve the imdb matching the specified movie name and year. This routine is base on a google
  * request.
  */
 private String getFilmAffinityId( String movieName, String year, int season )
 {
   try
   {     
  	 StringBuffer sb = new StringBuffer( "http://www.google.es/search?hl=es&q=");;
  	 sb.append( URLEncoder.encode (movieName, "UTF-8"));
  	 if (season != -1)
  		 sb.append("+TV");
   	 if ( year != null && !year.equalsIgnoreCase( Movie.UNKNOWN ) )
       sb.append( "+" ).append( year );

   	 sb.append("+site%3Awww.filmaffinity.com&btnG=Buscar+con+Google&meta=");

     String xml = webBrowser.request( sb.toString() );
     int beginIndex = xml.indexOf( "/es/film" );
     StringTokenizer st = new StringTokenizer( xml.substring( beginIndex + 8 ), "/\"" );
     String filmAffinityId = st.nextToken();

     //if ( imdbId.startsWith( "film" ) )
     if ( filmAffinityId != "")
     {
       return filmAffinityId;
     }
     else
     {
       return Movie.UNKNOWN;
     }

   }
   catch ( Exception e )
   {
     logger.severe( "Failed retreiving imdb Id for movie : " + movieName );
     logger.severe( "Error : " + e.getMessage() );
     return Movie.UNKNOWN;
   }
 }
 
 /**
  * Scan FilmAffinity html page for the specified movie
  */
 private void updateFilmAffinityMediaInfo( Movie movie, String filmAffinityId )
 {
   try
   {      
     String xml = webBrowser
         .request( "http://www.filmaffinity.com/es/film" + filmAffinityId );

     movie.setTitle( HTMLTools.extractTag( xml, "<td ><b>", 0, "()><-" ) );
     String plot = "None";
     plot = HTMLTools.extractTag( xml, "SINOPSIS:", 0, "><|" );
 
     if (plot.length() > preferredPlotLength)
   	  plot = plot.substring(0,preferredPlotLength) + "...";  
     
     movie.setPlot( plot );   

   }
   catch ( Exception e )
   {
     logger.severe( "Failed retreiving filmaffinity data movie : " + movie.getId( IMDB_PLUGIN_ID ) );
     e.printStackTrace();
   }
 }
 
 
}


