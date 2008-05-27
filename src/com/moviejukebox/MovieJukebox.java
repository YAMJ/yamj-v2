package com.moviejukebox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;

public class MovieJukebox {

	private String movieLibraryRoot;
	private String jukeboxRoot;
	private String detailsDirName;
	private String nmtRootPath;
	private boolean forceXMLOverwrite;
	private boolean forceHTMLOverwrite;
	private int thumbWidth;
	private int thumbHeight;
	private Properties props;

	public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
		
		String movieLibraryRoot = null;
		String jukeboxRoot = null;
		String nmtRootPath = "file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/";

		if (args.length==0) {
			help();
			return;
		} 
		
		try {
			for (int i = 0; i < args.length; i++) {
				String arg = (String) args[i];
				if ("-o".equalsIgnoreCase(arg)) {
					jukeboxRoot = args[++i]; 
				} else if ("-nr".equalsIgnoreCase(arg)) {
					nmtRootPath = args[++i]; 
				} else if (arg.startsWith("-")) {
					help();
					return;
				} else {
					movieLibraryRoot = args[i];
					if (jukeboxRoot == null) {
						jukeboxRoot = movieLibraryRoot;
					}
				}
			}
		} catch (Exception e) {
			System.err.println("Wrong arguments specified");
			help();
			return;
		}
		
		if (movieLibraryRoot == null) {
			help();
			return;
		}
		
		if (!new File(movieLibraryRoot).exists()) {
			System.err.println("Directory not found : " + movieLibraryRoot);
			return;
		}
		
		MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot, nmtRootPath);
		
		ml.generateLibrary();
	}

	private static void help() {
		System.out.println("Generates an HTML library for your movies library.");
		System.out.println("");
		System.out.println("MOVIELIB movieLibraryRoot [-o jukeboxRoot][-d detailsDirName][-nr nmtRootPath][-fx][-fh]");
		System.out.println("");
		System.out.println("    movieLibraryRoot    : MANDATORY");
		System.out.println("                          input directory (local or network directory)");
		System.out.println("                          This is where your movie files are stored.");
		System.out.println("");
		System.out.println("    -o jukeboxRoot      : OPTIONAL");
		System.out.println("                          output directory (local or network directory)");
		System.out.println("                          This is where the jukebox file will be written to");
		System.out.println("                          by default the is the same as the movieLibraryRoot");
		System.out.println("");
		System.out.println("    -nr nmtRootPath     : OPTIONAL");
		System.out.println("                          path of the media library root on your NMT");
		System.out.println("                          Default is: \"file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/\"");
	}

	private MovieJukebox(String movieLibraryRoot, String jukeboxRoot, String nmtRootPath) {
	    // Load moviejukebox.properties form the classpath
	    props = new java.util.Properties();
	    URL url = ClassLoader.getSystemResource("moviejukebox.properties");
	    try {
			props.load(url.openStream());
		} catch (IOException e) {
			System.err.println("Failed loading file moviejukebox.properties: Please check your configuration. The moviejukebox.properties should be in the classpath.");
			e.printStackTrace();
		}
	    System.out.println(props);
		
		this.movieLibraryRoot = movieLibraryRoot;
		this.jukeboxRoot = jukeboxRoot;
		this.detailsDirName = props.getProperty("mjb.detailsDirName", "Jukebox");
		this.forceXMLOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceXMLOverwrite", "true"));
		this.forceHTMLOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceHTMLOverwrite", "true"));
		this.nmtRootPath = nmtRootPath;
		
		try {
			this.thumbWidth = Integer.parseInt(props.getProperty("mjb.thumbnails.width", "140"));
		} catch (Exception e) {
			this.thumbWidth = 140;
		}
		
		try {
			this.thumbHeight = Integer.parseInt(props.getProperty("mjb.thumbnails.height", "200"));
		} catch (Exception e) {
			this.thumbHeight = 200;
		}
	}

	private void generateLibrary() throws FileNotFoundException, XMLStreamException {
		MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter(nmtRootPath, forceXMLOverwrite);
		MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter(forceHTMLOverwrite);
		MovieDatabasePlugin movieDB = new ImdbPlugin(props);
		MovieDirectoryScanner mds = new MovieDirectoryScanner();
		MovieNFOScanner nfoScanner = new MovieNFOScanner();
		MediaInfoScanner miScanner = new MediaInfoScanner(props);

		File mediaLibraryRoot = new File(movieLibraryRoot);
		String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;

		System.out.println("Scanning movies directory " + mediaLibraryRoot);
		Library library = mds.scan(mediaLibraryRoot);
		System.out.println("Found " + library.size() + " movies in your media library");
		
		System.out.println("Searching for movies information...");
		
		// retreiving internet Data
		for (Movie movie : library.values()) {
			
			// For each movie in the library, if an XML file for this 
			// movie already exist, then no need to search for movie 
			// information, just parse the XML data.
			File xmlFile = new File(jukeboxDetailsRoot + File.separator + movie.getBaseName() + ".xml");
			if (xmlFile.exists()) {
				// parse the movie XML file 
				xmlWriter.parseMovieXML(xmlFile, movie);
			} else {		
				// No XML file for this movie. We've got to find movie
				// information where we can (filename, IMDb, NFO, etc...)
				// Add here extra scanners if needed.
				nfoScanner.scan(movie);
				movieDB.scan(movie);
				miScanner.scan(movie);
				xmlWriter.writeMovieXML(jukeboxDetailsRoot, movie);
				System.out.println("Updating " + movie);
			}
			
			// Download poster file only if this file doesn't exist... 
			// never overwrite an existing file...
			movieDB.downloadPoster(jukeboxDetailsRoot, movie);
		}

		// Finally generate indexes and HTML jukebox files
		System.out.println("Generating Jukebox HTML files...");

		// library indexing
		library.buildIndex();

		for (Movie movie : library.values()) {
			xmlWriter.writeMovieXML(jukeboxDetailsRoot, movie);
			MovieJukeboxTools.createThumbnail(jukeboxDetailsRoot, movie, thumbWidth, thumbHeight);
			htmlWriter.generateMovieDetailsHTML(jukeboxDetailsRoot, movie);
		}
		
		xmlWriter.writeIndexXML(jukeboxDetailsRoot, detailsDirName, library);
		htmlWriter.generateMoviesIndexHTML(jukeboxRoot, detailsDirName, library);		
		
		MovieJukeboxTools.copyResource("exportdetails_item_popcorn.css", jukeboxDetailsRoot);
		MovieJukeboxTools.copyResource("exportindex_item_pch.css", jukeboxDetailsRoot);
		
		System.out.println("Process terminated.");
	}
}
