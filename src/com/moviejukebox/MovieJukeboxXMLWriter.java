package com.moviejukebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.sun.xml.internal.stream.events.CharacterEvent;

/**
 * Parse/Write XML files for movie details and library indexes
 * @author Julien
 */
public class MovieJukeboxXMLWriter implements parseCData {
	private boolean forceXMLOverwrite;
	private String nmtRootPath;

	public MovieJukeboxXMLWriter(String nmtRootPath, boolean forceXMLOverwrite) {
		this.forceXMLOverwrite = forceXMLOverwrite;
		this.nmtRootPath = nmtRootPath;
	}

	/**
	 * Parse a single movie detail xml file
	 */
	public void parseMovieXML(File xmlFile, Movie movie) {
		try {
			XMLInputFactory factory = XMLInputFactory.newInstance();    
			XMLEventReader r = factory.createXMLEventReader(
					new FileInputStream(xmlFile), "UTF-8");    
			
			if (movie.getTitle().startsWith("Florence Foresti fait des")) {
				System.out.println("oiuoiu");
			}
			
			while(r.hasNext()) {      
				XMLEvent e = r.nextEvent();
				String tag = e.toString();
				
				if (tag.equals("<id>")) { movie.setId(parseCData(r)); }
				if (tag.equals("<title>")) { movie.setTitle(parseCData(r)); }
				if (tag.equals("<titleSort>")) { movie.setTitleSort(parseCData(r)); }
				if (tag.equals("<year>")) { movie.setYear(parseCData(r)); }
				if (tag.equals("<releaseDate>")) { movie.setReleaseDate(parseCData(r)); }
				if (tag.equals("<rating>")) { movie.setRating(parseCData(r)); }
				if (tag.equals("<posterURL>")) { movie.setPosterURL(parseCData(r)); }
				if (tag.equals("<plot>")) { movie.setPlot(parseCData(r)); }
				if (tag.equals("<director>")) { movie.setDirector(parseCData(r)); }
				if (tag.equals("<country>")) { movie.setCountry(parseCData(r)); }
				if (tag.equals("<company>")) { movie.setCompany(parseCData(r)); }
				if (tag.equals("<runtime>")) { movie.setRuntime(parseCData(r)); }
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
				
				if (tag.equals("<genres>")) { 
					ArrayList<String> genres = new ArrayList<String>();
					e=r.nextEvent(); 
					if (!e.toString().equals("</genres>")) {
						genres.add(e.toString());
						movie.setGenres(genres); 
					}
				}
				
				if (tag.equals("<cast>")) { 
					ArrayList<String> cast = new ArrayList<String>();
					e=r.nextEvent(); 
					if (!e.toString().equals("</cast>")) {
						cast.add(e.toString());
						movie.setCasting(cast); 
					}
				}
				
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
	}

	private String parseCData(XMLEventReader r) throws XMLStreamException {
		StringBuffer sb = new StringBuffer();
		XMLEvent e;
		while( (e=r.nextEvent()) instanceof CharacterEvent) {
			sb.append(e.toString());
		}
		return sb.toString();
	}
	
	
	/**
	 * Write the set of index XML files for the library
	 */
	public void writeIndexXML(String rootPath, String detailsDirName, Library library) throws FileNotFoundException, XMLStreamException {
		ArrayList<String> keys = new ArrayList<String>();
		keys.addAll(library.getIndexes().keySet());
		Collections.sort(keys);
		
		for (String key : keys) {
			File xmlFile = new File(rootPath + "/index_" + key + ".xml");
			xmlFile.getParentFile().mkdirs();
				
			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter writer = outputFactory.createXMLStreamWriter(
					new FileOutputStream(xmlFile), "UTF-8");

			writer.writeStartDocument();
			writer.writeStartElement("library");

			writer.writeStartElement("indexes");
			for (String akey : keys) {
				writer.writeStartElement("index"); 
				writer.writeAttribute("name", akey);
				writer.writeCharacters("index_" + akey); 
				writer.writeEndElement();
			}				
			writer.writeEndElement();					

			writer.writeStartElement("movies");
			List<Movie> movies = library.getIndexes().get(key);
			for (Movie movie : movies) {
				writer.writeStartElement("movie"); 
				writer.writeStartElement("title"); writer.writeCharacters(movie.getTitleSort()); writer.writeEndElement();
				writer.writeStartElement("details"); writer.writeCharacters(detailsDirName + "/" + movie.getBaseName()); writer.writeEndElement();
				writer.writeEndElement();
			}
			writer.writeEndElement();					
			
			writer.writeEndElement();					
			writer.writeEndDocument();
			writer.close();
		}
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
			
			System.err.println(movie);
			XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
			XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileOutputStream(xmlFile), "UTF-8");
	
			writer.writeStartDocument();
			writer.writeStartElement("movie");			
			writer.writeStartElement("id"); writer.writeCharacters(movie.getId()); writer.writeEndElement();
			writer.writeStartElement("title"); writer.writeCharacters(movie.getTitle()); writer.writeEndElement();
			writer.writeStartElement("titleSort"); writer.writeCharacters(movie.getTitleSort()); writer.writeEndElement();
			writer.writeStartElement("year"); writer.writeCharacters(movie.getYear()); writer.writeEndElement();
			writer.writeStartElement("releaseDate"); writer.writeCharacters(movie.getReleaseDate()); writer.writeEndElement();
			writer.writeStartElement("rating"); writer.writeCharacters(movie.getRating()); writer.writeEndElement();
			writer.writeStartElement("posterURL"); writer.writeCharacters(movie.getPosterURL()); writer.writeEndElement();
			writer.writeStartElement("posterFile"); writer.writeCharacters(movie.getBaseName() + ".jpg"); writer.writeEndElement();
			writer.writeStartElement("plot"); writer.writeCharacters(movie.getPlot()); writer.writeEndElement();
			writer.writeStartElement("director"); writer.writeCharacters(movie.getDirector()); writer.writeEndElement();
			writer.writeStartElement("country"); writer.writeCharacters(movie.getCountry()); writer.writeEndElement();
			writer.writeStartElement("company"); writer.writeCharacters(movie.getCompany()); writer.writeEndElement();
			writer.writeStartElement("runtime"); writer.writeCharacters(movie.getRuntime()); writer.writeEndElement();
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
			
			StringBuffer genres = new StringBuffer();
			for (String genre : movie.getGenres()) {
				genres.append(genre);
				genres.append(" ");
			}
			
			writer.writeStartElement("genres"); writer.writeCharacters(genres.toString().trim()); writer.writeEndElement();
	
			StringBuffer cast = new StringBuffer();
			for (String actor : movie.getCasting()) {
				cast.append(actor);
				cast.append(" ");
			}
			
			writer.writeStartElement("cast"); writer.writeCharacters(cast.toString().trim()); writer.writeEndElement();
	
			writer.writeStartElement("files"); 
			for (MovieFile mf : movie.getFiles()) { 
				writer.writeStartElement("file");
				writer.writeAttribute("part", Integer.toString(mf.getPart()));
				
				if (movie.isTVShow()) {
					writer.writeAttribute("title", "Episode " + mf.getPart() + ((mf.getTitle().equalsIgnoreCase("UNKNOWN"))?(""): (" - " + mf.getTitle())));
				} else {
					if(movie.getFiles().size()==1) {
						writer.writeAttribute("title", movie.getBaseName());
					} else {
						int index = mf.getFilename().lastIndexOf("/");
						if (index != -1) {
							String filename = mf.getFilename().substring(index+1);
							writer.writeAttribute("title", filename);
						} else {
							writer.writeAttribute("title", "Part " + mf.getPart());
						}
					}
				}
				writer.writeCharacters(nmtRootPath + mf.getFilename()); 
				writer.writeEndElement();
			}
			writer.writeEndElement();
				
			writer.writeEndElement();
			writer.writeEndDocument();
			writer.close();
		}
	}
}
