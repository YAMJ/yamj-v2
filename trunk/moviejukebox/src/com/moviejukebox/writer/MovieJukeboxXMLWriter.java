package com.moviejukebox.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.tools.HTMLTools;

/**
 * Parse/Write XML files for movie details and library indexes
 * @author Julien
 */
public class MovieJukeboxXMLWriter {
	
	private boolean forceXMLOverwrite;
	private int nbMoviesPerPage;
	private int nbMoviesPerLine;
	private String homePage;
	private boolean fullMovieInfoInIndexes;

	public MovieJukeboxXMLWriter(Properties props) {
		forceXMLOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceXMLOverwrite", "false"));
		nbMoviesPerPage = Integer.parseInt(props.getProperty("mjb.nbThumbnailsPerPage", "10"));
		nbMoviesPerLine = Integer.parseInt(props.getProperty("mjb.nbThumbnailsPerLine", "5"));
		homePage = props.getProperty("mjb.homePage", "Other_All_1") + ".html";
		fullMovieInfoInIndexes = Boolean.parseBoolean(props.getProperty("mjb.fullMovieInfoInIndexes","false"));
	}

	/**
	 * Parse a single movie detail xml file
	 */
	@SuppressWarnings("unchecked")
	public void parseMovieXML(File xmlFile, Movie movie) {
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();    
			XMLEventReader r = factory.createXMLEventReader(new FileInputStream(xmlFile), "UTF-8");    
			
			while(r.hasNext()) {      
				XMLEvent e = r.nextEvent();
				String tag = e.toString();
				
				if (tag.startsWith("<id ")) {
					String movieDatabase = ImdbPlugin.IMDB_PLUGIN_ID;
					StartElement start = e.asStartElement();
					for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
						Attribute attr = i.next();
						String ns = attr.getName().toString();

						if ("movieDatabase".equals(ns)) {
							movieDatabase = ns;
							continue;
						}
					}
					movie.setId(movieDatabase, parseCData(r));
				}
				if (tag.equals("<title>")) { movie.setTitle(parseCData(r)); }
				if (tag.equals("<titleSort>")) { movie.setTitleSort(parseCData(r)); }
				if (tag.equals("<year>")) { movie.setYear(parseCData(r)); }
				if (tag.equals("<releaseDate>")) { movie.setReleaseDate(parseCData(r)); }
				if (tag.equals("<rating>")) { movie.setRating(Integer.parseInt(parseCData(r))); }
				if (tag.equals("<posterURL>")) { movie.setPosterURL(parseCData(r)); }
				if (tag.equals("<posterFile>")) { movie.setPosterFilename(parseCData(r)); }
                if (tag.equals("<detailPosterFile>")) { movie.setDetailPosterFilename(parseCData(r)); }
				if (tag.equals("<thumbnailFile>")) { movie.setThumbnailFilename(parseCData(r)); }
				if (tag.equals("<plot>")) { movie.setPlot(parseCData(r)); }
				if (tag.equals("<director>")) { movie.setDirector(parseCData(r)); }
				if (tag.equals("<country>")) { movie.setCountry(parseCData(r)); }
				if (tag.equals("<company>")) { movie.setCompany(parseCData(r)); }
				if (tag.equals("<runtime>")) { movie.setRuntime(parseCData(r)); }
				if (tag.equals("<genre>")) { movie.addGenre(parseCData(r)); } 
				if (tag.equals("<actor>")) { movie.addActor(parseCData(r)); } 
				if (tag.equals("<certification>")) { movie.setCertification(parseCData(r)); }
				if (tag.equals("<season>")) { movie.setSeason(Integer.parseInt(parseCData(r))); }
				if (tag.equals("<language>")) { movie.setLanguage(parseCData(r)); }
				if (tag.equals("<subtitles>")) { movie.setSubtitles(parseCData(r).equalsIgnoreCase("YES")); }
				if (tag.equals("<container>")) { movie.setContainer(parseCData(r)); }
				if (tag.equals("<videoCodec>")) { movie.setVideoCodec(parseCData(r)); }
				if (tag.equals("<audioCodec>")) { movie.setAudioCodec(parseCData(r)); }
				if (tag.equals("<resolution>")) { movie.setResolution(parseCData(r)); }
				if (tag.equals("<videoSource>")) { movie.setVideoSource(parseCData(r)); }
				if (tag.equals("<videoOutput>")) { movie.setVideoOutput(parseCData(r)); }
				if (tag.equals("<fps>")) { movie.setFps(Integer.parseInt(parseCData(r))); }
				if (tag.equals("<first>")) { movie.setFirst(parseCData(r)); }
				if (tag.equals("<previous>")) { movie.setPrevious(parseCData(r)); }
				if (tag.equals("<next>")) { movie.setNext(parseCData(r)); }
				if (tag.equals("<last>")) { movie.setLast(parseCData(r)); }
				
				if (tag.startsWith("<file ")) {
					MovieFile mf = new MovieFile();
	
					StartElement start = e.asStartElement();
					for (Iterator<Attribute> i = start.getAttributes(); i.hasNext();) {
	                    Attribute attr = i.next();
	                    String ns = attr.getName().toString();
	
	                    if ("title".equals(ns)) {
	                    	mf.setTitle(attr.getValue());
	                        continue;
					    }
	                    
	                    if ("part".equals(ns)) {
	                  	  	mf.setPart(Integer.parseInt(attr.getValue()));
	                        continue;
	                    }
					}
					
					mf.setFilename(parseCData(r));
					movie.addMovieFile(mf);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed parsing " + xmlFile.getAbsolutePath() + " : please fix it or remove it.");
		}
		
		movie.setDirty(false);
	}

	private String parseCData(XMLEventReader r) throws XMLStreamException {
		StringBuffer sb = new StringBuffer();
		XMLEvent e;
		while( (e=r.nextEvent()) instanceof Characters) {
			sb.append(e.toString());
		}
		return HTMLTools.decodeHtml(sb.toString());
	}
	
	public void writeCategoryXML( String rootPath, String detailsDirName, Library library )
		throws FileNotFoundException, XMLStreamException
	{
		File folder = new File( rootPath, detailsDirName );
		folder.mkdirs();

		File xmlFile = new File( folder, "Categories.xml" );

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = outputFactory.createXMLStreamWriter( new FileOutputStream( xmlFile ),
				"UTF-8" );

		writer.writeStartDocument();
		writer.writeStartElement( "library" );

		for ( Map.Entry< String, Map< String, List< Movie >>> category : library.getIndexes().entrySet())
		{
			writer.writeStartElement( "category" );
			writer.writeAttribute( "name", category.getKey());
			
			for ( Map.Entry< String, List< Movie >> index : category.getValue().entrySet())
			{
				writer.writeStartElement("index");
				writer.writeCharacters( index.getKey());
				writer.writeEndElement();
			}
			writer.writeEndElement(); // category
		}
		writer.writeEndElement(); // library
		writer.writeEndDocument();
		writer.close();
	}

	/**
	 * Write the set of index XML files for the library
	 */
	public void writeIndexXML(String rootPath, String detailsDirName, Library library)
	    throws FileNotFoundException, XMLStreamException {
		
		for ( Map.Entry<String, Map<String,List<Movie>>> category : library.getIndexes().entrySet())
		{
			String categoryName = category.getKey();
			Map< String, List<Movie>> index = category.getValue();
			
			for ( Map.Entry< String, List<Movie>> group : index.entrySet()) {
				String key = group.getKey(); 
				List<Movie> movies = group.getValue();

				int previous = 1;
				int current = 1;
				int last;
				
				if (movies.size() % nbMoviesPerPage != 0) {
					last = 1 + movies.size() / nbMoviesPerPage;
				} else {
					last = movies.size() / nbMoviesPerPage;
				}
				
				int next = Math.min(2, last);
				int nbMoviesLeft = nbMoviesPerPage;
				
				List<Movie> moviesInASinglePage = new ArrayList<Movie>();
				for (Movie movie : movies) {
					moviesInASinglePage.add(movie);
					nbMoviesLeft--;
				
					if (nbMoviesLeft == 0) {
						writeIndexPage(library, moviesInASinglePage, rootPath, categoryName, key, previous, current, next, last);
						moviesInASinglePage = new ArrayList<Movie>();
						previous = current;
						current = Math.min(current+1, last);
						next = Math.min(current+1, last);
						nbMoviesLeft = nbMoviesPerPage;
					}
				}

				if (moviesInASinglePage.size() > 0) {
					writeIndexPage(library, moviesInASinglePage, rootPath, categoryName, key, previous, current, next, last);
				}
			}
		}
	}
	
	private String createPrefix( String category, String key )
	{
		return category + '_' + key + '_';
	}

	public void writeIndexPage(Library library, Collection<Movie> movies, String rootPath, String categoryName, String key, int previous, int current, int next, int last) throws FileNotFoundException, XMLStreamException {
		String prefix = createPrefix( categoryName, key );
		File xmlFile = new File(rootPath,  prefix + current + ".xml");
		xmlFile.getParentFile().mkdirs();

		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileOutputStream(xmlFile), "UTF-8");

		writer.writeStartDocument("UTF-8", "1.0");
		writer.writeStartElement("library");
		writePreferences( writer );

		for ( Map.Entry<String, Map<String,List<Movie>>> category : library.getIndexes().entrySet())
		{
			String categoryKey = category.getKey();
			Map< String, List<Movie>> index = category.getValue();
			writer.writeStartElement("category");
			writer.writeAttribute("name", categoryKey);
			if ( categoryKey.equals( categoryName ))
			{
				writer.writeAttribute( "current", "true" );
			}

			for (String akey : index.keySet()) {
				prefix = createPrefix( categoryKey, akey );
				
				writer.writeStartElement("index"); 
				writer.writeAttribute("name", akey);

				// if currently writing this page then add current attribute with value true
				if(akey.equals(key)) {
					writer.writeAttribute("current", "true");
					writer.writeAttribute("first", prefix + '1');
					writer.writeAttribute("previous", prefix + previous);
					writer.writeAttribute("next", prefix + next);
					writer.writeAttribute("last", prefix + last );
					writer.writeAttribute("currentIndex", Integer.toString(current));
					writer.writeAttribute("lastIndex", Integer.toString(last));
				}

				writer.writeCharacters( prefix + '1'); 
				writer.writeEndElement(); // index
			}

			writer.writeEndElement(); // categories					
		}
		writer.writeStartElement("movies");
		writer.writeAttribute("count", ""+ nbMoviesPerPage);
		writer.writeAttribute("cols", ""+ nbMoviesPerLine);

        if (fullMovieInfoInIndexes) {
    		for (Movie movie : movies) {
    			writeMovie( writer, movie ); 
    		}
        } 
        else {
    		for (Movie movie : movies) {
    			writeMovieForIndex( writer, movie ); 
    		}
		}
		writer.writeEndElement(); // movies					

		writer.writeEndElement(); // library 					
		writer.writeEndDocument();
		writer.close();
	}
	
	
	   private void writeMovieForIndex( XMLStreamWriter writer, Movie movie ) throws XMLStreamException
	   {
			writer.writeStartElement("movie");			
			writer.writeStartElement("details"); writer.writeCharacters( movie.getBaseName() + ".html" ); writer.writeEndElement();
			writer.writeStartElement("title"); writer.writeCharacters(movie.getTitle()); writer.writeEndElement();
			writer.writeStartElement("titleSort"); writer.writeCharacters(movie.getTitleSort()); writer.writeEndElement();
			writer.writeStartElement("detailPosterFile"); writer.writeCharacters(movie.getDetailPosterFilename()); writer.writeEndElement();
			writer.writeStartElement("thumbnail"); writer.writeCharacters(movie.getThumbnailFilename()); writer.writeEndElement();
			writer.writeStartElement("certification"); writer.writeCharacters(movie.getCertification()); writer.writeEndElement();
			writer.writeStartElement("season"); writer.writeCharacters(Integer.toString(movie.getSeason())); writer.writeEndElement();
						
			writer.writeEndElement();

	   }

   private void writeMovie( XMLStreamWriter writer, Movie movie ) throws XMLStreamException
   {
		writer.writeStartElement("movie");			
		for (Map.Entry<String, String> e : movie.getIdMap().entrySet()) {
			writer.writeStartElement("id");
			writer.writeAttribute("movieDatabase", e.getKey());
			writer.writeCharacters(e.getValue());
			writer.writeEndElement();
		}
		writer.writeStartElement("title"); writer.writeCharacters(movie.getTitle()); writer.writeEndElement();
		writer.writeStartElement("titleSort"); writer.writeCharacters(movie.getTitleSort()); writer.writeEndElement();
		writer.writeStartElement("year"); writer.writeCharacters(movie.getYear()); writer.writeEndElement();
		writer.writeStartElement("releaseDate"); writer.writeCharacters(movie.getReleaseDate()); writer.writeEndElement();
		writer.writeStartElement("rating"); writer.writeCharacters(Integer.toString(movie.getRating())); writer.writeEndElement();
		writer.writeStartElement("details"); writer.writeCharacters( movie.getBaseName() + ".html" ); writer.writeEndElement();
		writer.writeStartElement("posterURL"); writer.writeCharacters(movie.getPosterURL()); writer.writeEndElement();
		writer.writeStartElement("posterFile"); writer.writeCharacters(movie.getPosterFilename()); writer.writeEndElement();
		writer.writeStartElement("detailPosterFile"); writer.writeCharacters(movie.getDetailPosterFilename()); writer.writeEndElement();
		writer.writeStartElement("thumbnail"); writer.writeCharacters(movie.getThumbnailFilename()); writer.writeEndElement();
		writer.writeStartElement("plot"); writer.writeCharacters(movie.getPlot()); writer.writeEndElement();
		writer.writeStartElement("director"); writer.writeCharacters(movie.getDirector()); writer.writeEndElement();
		writer.writeStartElement("country"); writer.writeCharacters(movie.getCountry()); writer.writeEndElement();
		writer.writeStartElement("company"); writer.writeCharacters(movie.getCompany()); writer.writeEndElement();
		writer.writeStartElement("runtime"); writer.writeCharacters(movie.getRuntime()); writer.writeEndElement();
		writer.writeStartElement("certification"); writer.writeCharacters(movie.getCertification()); writer.writeEndElement();
		writer.writeStartElement("season"); writer.writeCharacters(Integer.toString(movie.getSeason())); writer.writeEndElement();
		writer.writeStartElement("language"); writer.writeCharacters(movie.getLanguage()); writer.writeEndElement();
		writer.writeStartElement("subtitles"); writer.writeCharacters(movie.hasSubtitles()?"YES":"NO"); writer.writeEndElement();
		writer.writeStartElement("container"); writer.writeCharacters(movie.getContainer()); writer.writeEndElement();  // AVI, MKV, TS, etc.
		writer.writeStartElement("videoCodec"); writer.writeCharacters(movie.getVideoCodec()); writer.writeEndElement(); // DIVX, XVID, H.264, etc.
		writer.writeStartElement("audioCodec"); writer.writeCharacters(movie.getAudioCodec()); writer.writeEndElement(); // MP3, AC3, DTS, etc.
		writer.writeStartElement("resolution"); writer.writeCharacters(movie.getResolution()); writer.writeEndElement(); // 1280x528
		writer.writeStartElement("videoSource"); writer.writeCharacters(movie.getVideoSource()); writer.writeEndElement();
		writer.writeStartElement("videoOutput"); writer.writeCharacters(movie.getVideoOutput()); writer.writeEndElement();
		writer.writeStartElement("fps"); writer.writeCharacters(Integer.toString(movie.getFps())); writer.writeEndElement();

		writer.writeStartElement("first"); writer.writeCharacters(movie.getFirst()); writer.writeEndElement();
		writer.writeStartElement("previous"); writer.writeCharacters(movie.getPrevious()); writer.writeEndElement();
		writer.writeStartElement("next"); writer.writeCharacters(movie.getNext()); writer.writeEndElement();
		writer.writeStartElement("last"); writer.writeCharacters(movie.getLast()); writer.writeEndElement();
		
		Collection< String> items = movie.getGenres();
		if ( items.size()> 0 ) {
			writer.writeStartElement("genres");	
			for (String genre : items ) {
				writer.writeStartElement("genre");	
				writer.writeCharacters(genre);
				writer.writeEndElement();
			}
			writer.writeEndElement();
		}
		
		items = movie.getCast();
		if ( items.size()> 0 ) {
			writer.writeStartElement("cast");	
			for (String genre : items ) {
				writer.writeStartElement("actor");	
				writer.writeCharacters(genre);
				writer.writeEndElement();
			}
			writer.writeEndElement();
		}
		
		writer.writeStartElement("files"); 
		for (MovieFile mf : movie.getFiles()) { 
			writer.writeStartElement("file");
			writer.writeAttribute("part", Integer.toString(mf.getPart()));
			
			if (movie.isTVShow()) {
				writer.writeAttribute("title", "Episode " + mf.getPart() + ((mf.getTitle().equalsIgnoreCase("UNKNOWN"))?(""): (" - " + mf.getTitle())));
			} else {
				writer.writeAttribute("title", movie.getTitle() + " (Part " + mf.getPart() + ")");
			}
			writer.writeCharacters(mf.getNmtRootPath() + mf.getFilename()); 
			writer.writeEndElement();
		}
		writer.writeEndElement();
			
		writer.writeEndElement();

   }
   
   public void writePreferences( XMLStreamWriter writer ) throws XMLStreamException
   {
		writer.writeStartElement( "preferences" );
		writer.writeStartElement( "homePage" );
		writer.writeCharacters( homePage );
        writer.writeEndElement();
        writer.writeEndElement();
   }
	/**
	 * Persist a movie into an XML file.
	 * Doesn't overwrite an already existing XML file for the specified movie
	 * unless, movie's data has changed or forceXMLOverwrite is true.
	 */
	public void writeMovieXML(String rootPath, Movie movie) throws FileNotFoundException, XMLStreamException {
		File xmlFile = new File(rootPath + File.separator + movie.getBaseName() + ".xml");
		xmlFile.getParentFile().mkdirs();
		
		if (!xmlFile.exists() || forceXMLOverwrite || movie.isDirty()) {
			
			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileOutputStream(xmlFile), "UTF-8");
	
			writer.writeStartDocument("UTF-8", "1.0");
			writer.writeStartElement("details");
			writePreferences( writer );
			writeMovie( writer, movie );
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.close();
		}
	}	
}
