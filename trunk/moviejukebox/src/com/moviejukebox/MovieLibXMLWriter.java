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

public class MovieLibXMLWriter {

	private boolean forceXMLOverwrite;
	private String nmtRootPath;

	public MovieLibXMLWriter(String nmtRootPath, boolean forceXMLOverwrite) {
		this.forceXMLOverwrite = forceXMLOverwrite;
		this.nmtRootPath = nmtRootPath;
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
			
			if (!xmlFile.exists() || forceXMLOverwrite) {
				xmlFile.getParentFile().mkdirs();
				
				XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
				
				XMLStreamWriter writer = 
					outputFactory.createXMLStreamWriter(
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
	}

	/**
	 * Write a single movie detail file
	 */
	public void writeMovieXML(String rootPath, Movie movie) throws FileNotFoundException, XMLStreamException {
		File xmlFile = new File(rootPath + File.separator + movie.getBaseName() + ".xml");
		xmlFile.getParentFile().mkdirs();
		
		if (!xmlFile.exists() || forceXMLOverwrite) {
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
					writer.writeAttribute("title", movie.getFiles().size()==1? movie.getBaseName() :"Part " + mf.getPart() + " : " + movie.getBaseName());
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

	/**
	 * Parse a single movie detail xml file
	 */
	public void parseMovieXML(File xmlFile, Movie movie) {
		try {
		XMLInputFactory factory = XMLInputFactory.newInstance();    
		XMLEventReader r = factory.createXMLEventReader(
			new FileInputStream(xmlFile), "UTF-8");    
			while(r.hasNext()) {      
				XMLEvent e = r.nextEvent();
				String tag = e.toString();
				if (tag.equals("<id>")) { e=r.nextEvent(); movie.setId(e.toString()); }
				if (tag.equals("<title>")) { e=r.nextEvent(); movie.setTitle(e.toString()); }
				if (tag.equals("<titleSort>")) { e=r.nextEvent(); movie.setTitleSort(e.toString()); }
				if (tag.equals("<year>")) { e=r.nextEvent(); movie.setYear(e.toString()); }
				if (tag.equals("<releaseDate>")) { e=r.nextEvent(); movie.setReleaseDate(e.toString()); }
				if (tag.equals("<rating>")) { e=r.nextEvent(); movie.setRating(e.toString()); }
				if (tag.equals("<posterURL>")) { e=r.nextEvent(); movie.setPosterURL(e.toString()); }
				if (tag.equals("<plot>")) { e=r.nextEvent(); movie.setPlot(e.toString()); }
				if (tag.equals("<director>")) { e=r.nextEvent(); movie.setDirector(e.toString()); }
				if (tag.equals("<country>")) { e=r.nextEvent(); movie.setCountry(e.toString()); }
				if (tag.equals("<company>")) { e=r.nextEvent(); movie.setCompany(e.toString()); }
				if (tag.equals("<runtime>")) { e=r.nextEvent(); movie.setRuntime(e.toString()); }
				if (tag.equals("<season>")) { e=r.nextEvent(); movie.setSeason(Integer.parseInt(e.toString())); }
				if (tag.equals("<language>")) { e=r.nextEvent(); movie.setLanguage(e.toString()); }
				if (tag.equals("<subtitles>")) { e=r.nextEvent(); movie.setSubtitles(e.toString().equalsIgnoreCase("YES")); }
				if (tag.equals("<container>")) { e=r.nextEvent(); movie.setContainer(e.toString()); }
				if (tag.equals("<videoCodec>")) { e=r.nextEvent(); movie.setVideoCodec(e.toString()); }
				if (tag.equals("<audioCodec>")) { e=r.nextEvent(); movie.setAudioCodec(e.toString()); }
				if (tag.equals("<resolution>")) { e=r.nextEvent(); movie.setResolution(e.toString()); }
				if (tag.equals("<videoSource>")) { e=r.nextEvent(); movie.setVideoSource(e.toString()); }
				if (tag.equals("<videoOutput>")) { e=r.nextEvent(); movie.setVideoOutput(e.toString()); }
				if (tag.equals("<fps>")) { e=r.nextEvent(); movie.setFps(Integer.parseInt(e.toString())); }
				if (tag.equals("<first>")) { e=r.nextEvent(); movie.setFirst(e.toString()); }
				if (tag.equals("<previous>")) { e=r.nextEvent(); movie.setPrevious(e.toString()); }
				if (tag.equals("<next>")) { e=r.nextEvent(); movie.setNext(e.toString()); }
				if (tag.equals("<last>")) { e=r.nextEvent(); movie.setLast(e.toString()); }
				
				if (tag.equals("<genres>")) { 
					ArrayList<String> genres = new ArrayList<String>();
					e=r.nextEvent(); 
					genres.add(e.toString());
					movie.setGenres(genres); 
				}
				
				if (tag.equals("<cast>")) { 
					ArrayList<String> cast = new ArrayList<String>();
					e=r.nextEvent(); 
					cast.add(e.toString());
					movie.setGenres(cast); 
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
	
					e=r.nextEvent(); 
					mf.setFilename(e.toString());
					movie.addMovieFile(mf);
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Failed parsing " + xmlFile.getAbsolutePath() + "please fix it or remove it.");
		}
	}
}
