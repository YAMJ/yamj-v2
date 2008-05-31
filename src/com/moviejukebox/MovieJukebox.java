package com.moviejukebox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;

public class MovieJukebox {

	private static Logger logger = Logger.getLogger("moviejukebox");

	Collection<MediaLibraryPath> movieLibraryPaths;
	
	private String movieLibraryRoot;
	private String jukeboxRoot;
	private String detailsDirName;
	private boolean forceXMLOverwrite;
	private boolean forceHTMLOverwrite;
	private int thumbWidth;
	private int thumbHeight;
	private Properties props;

	public static void main(String[] args) throws XMLStreamException, SecurityException, IOException {
		// Send logger output to our FileHandler.
		
		Formatter mjbFormatter = new Formatter() { 
			public synchronized String format(LogRecord record) {
				return record.getMessage() + 
					(String) java.security.AccessController.doPrivileged(
			               new sun.security.action.GetPropertyAction("line.separator"));
			}
		};

		FileHandler fh = new FileHandler("moviejukebox.log");
		fh.setFormatter(mjbFormatter);
		fh.setLevel(Level.ALL);
		
		ConsoleHandler ch = new ConsoleHandler();
		ch.setFormatter(mjbFormatter);
		ch.setLevel(Level.FINE);
		
		logger.setUseParentHandlers(false);
		logger.addHandler(fh);
		logger.addHandler(ch);
		logger.setLevel(Level.ALL);

		String movieLibraryRoot = null;
		String jukeboxRoot = null;

		if (args.length == 0) {
			help();
			return;
		}

		try {
			for (int i = 0; i < args.length; i++) {
				String arg = (String) args[i];
				if ("-o".equalsIgnoreCase(arg)) {
					jukeboxRoot = args[++i];
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

		MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot);
		ml.generateLibrary();
	}

	private static void help() {
		System.out.println("Generates an HTML library for your movies library.");
		System.out.println("");
		System.out.println("MOVIELIB movieLibraryRoot [-o jukeboxRoot]");
		System.out.println("");
		System.out.println("    movieLibraryRoot    : MANDATORY");
		System.out.println("                          This parameter can be either: ");
		System.out.println("                          - An existing directory (local or network)");
		System.out.println("                            This is where your movie files are stored.");
		System.out.println("");
		System.out.println("                          - Or an XML configuration file specifying one or");
		System.out.println("                            many directories to be scanned for movies.");
		System.out.println("                            Please check README.TXT for further information.");
		System.out.println("");
		System.out.println("    -o jukeboxRoot      : OPTIONAL");
		System.out.println("                          output directory (local or network directory)");
		System.out.println("                          This is where the jukebox file will be written to");
		System.out.println("                          by default the is the same as the movieLibraryRoot");
	}

	private MovieJukebox(String source, String jukeboxRoot) {
		// Load moviejukebox.properties form the classpath
		props = new java.util.Properties();
		URL url = ClassLoader.getSystemResource("moviejukebox.properties");
		try {
			props.load(url.openStream());
		} catch (IOException e) {
			logger.severe("Failed loading file moviejukebox.properties: Please check your configuration. The moviejukebox.properties should be in the classpath.");
			e.printStackTrace();
		}

		System.out.println(props);

		this.movieLibraryRoot = source;
		this.jukeboxRoot = jukeboxRoot;
		this.detailsDirName = props.getProperty("mjb.detailsDirName", "Jukebox");
		this.forceXMLOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceXMLOverwrite", "true"));
		this.forceHTMLOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceHTMLOverwrite", "true"));

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
		

		File f = new File(source);
		if (f.exists() && f.isFile() && source.toUpperCase().endsWith("XML")) {
			movieLibraryPaths = parseMovieLibraryRootFile(f);
		} else if (f.exists() && f.isDirectory()) {
			movieLibraryPaths = new ArrayList<MediaLibraryPath>();
			MediaLibraryPath mlp = new MediaLibraryPath();
			mlp.setPath(source);
			mlp.setNmtRootPath(props.getProperty("mjb.nmtRootPath", "file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/"));
			mlp.setExcludes(new ArrayList<String>());
			movieLibraryPaths.add(mlp);
		}
	}

	private void generateLibrary() throws FileNotFoundException, XMLStreamException {
		MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter(forceXMLOverwrite);
		MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter(forceHTMLOverwrite);
		MovieDatabasePlugin movieDB = new ImdbPlugin(props);
		MovieDirectoryScanner mds = new MovieDirectoryScanner(props);
		MovieNFOScanner nfoScanner = new MovieNFOScanner();
		MediaInfoScanner miScanner = new MediaInfoScanner(props);

		File mediaLibraryRoot = new File(movieLibraryRoot);
		String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;

		logger.fine("Scanning movies directory " + mediaLibraryRoot);
		Library library = new Library();
		
		for (MediaLibraryPath mediaLibraryPath : movieLibraryPaths) {
			logger.finer("Scanning media library " + mediaLibraryPath.getPath());
			mds.scan(mediaLibraryPath, library);
		}

		logger.fine("Found " + library.size() + " movies in your media library");

		logger.fine("Searching for movies information...");

		// retreiving internet Data
		for (Movie movie : library.values()) {
			logger.fine("Processing movie: " + movie.getTitle());

			// For each movie in the library, if an XML file for this
			// movie already exist, then no need to search for movie
			// information, just parse the XML data.
			File xmlFile = new File(jukeboxDetailsRoot + File.separator + movie.getBaseName() + ".xml");
			if (xmlFile.exists()) {
				// parse the movie XML file
				logger.finer("movie XML file found for movie:" + movie.getBaseName());
				xmlWriter.parseMovieXML(xmlFile, movie);
			} else {
				// No XML file for this movie. We've got to find movie
				// information where we can (filename, IMDb, NFO, etc...)
				// Add here extra scanners if needed.
				logger.finer("movie XML file not found. Scanning Internet Data for file " + movie.getBaseName());
				nfoScanner.scan(movie);
				movieDB.scan(movie);
				miScanner.scan(movie);
				xmlWriter.writeMovieXML(jukeboxDetailsRoot, movie);
				logger.finer("movie XML file created for movie:" + movie.getBaseName());
			}

			// Download poster file only if this file doesn't exist...
			// never overwrite an existing file...
			logger.finer("Downloading poster file for movie " + movie.getTitle() + "...");
			movieDB.downloadPoster(jukeboxDetailsRoot, movie);
		}

		// library indexing
		logger.fine("Indexing libraries...");
		library.buildIndex();

		logger.fine("Creating movie XML files...");
		for (Movie movie : library.values()) {
			xmlWriter.writeMovieXML(jukeboxDetailsRoot, movie);
			logger.finest("Creating thumbnails for movie: " + movie.getBaseName());
			MovieJukeboxTools.createThumbnail(jukeboxDetailsRoot, movie, thumbWidth, thumbHeight);
			htmlWriter.generateMovieDetailsHTML(jukeboxDetailsRoot, movie);
		}

		logger.fine("Generating Indexes...");
		xmlWriter.writeIndexXML(jukeboxDetailsRoot, detailsDirName, library);
		htmlWriter.generateMoviesIndexHTML(jukeboxRoot, detailsDirName, library);

		logger.fine("Copying resources to Jukebox directory...");
		MovieJukeboxTools.copyResource("exportdetails_item_popcorn.css", jukeboxDetailsRoot);
		MovieJukeboxTools.copyResource("exportindex_item_pch.css", jukeboxDetailsRoot);

		logger.fine("Process terminated.");
	}

	private Collection<MediaLibraryPath> parseMovieLibraryRootFile(File f) {
		Collection<MediaLibraryPath> movieLibraryPaths = 
			new ArrayList<MediaLibraryPath>();
			
		XMLConfiguration c = new XMLConfiguration(f);
		
		List fields = c.configurationsAt("libraries.libray");
			
		for(Iterator it = fields.iterator(); it.hasNext();) {
		    HierarchicalConfiguration sub = (HierarchicalConfiguration) it.next();
		    // sub contains now all data about a single medialibrary node
		    String path = sub.getString("path");
		    String nmtpath = sub.getString("nmtpath");
		    String excludesString = sub.getString("excludes");
			    
		    StringTokenizer st = new StringTokenizer(",");
		    Collection<String> excludes = new ArrayList<String>();
		    while (st.hasMoreTokens()) {
		    	excludes.add(st.nextToken());
		    }
		    
		    MediaLibraryPath medlib = new MediaLibraryPath();
		    medlib.setPath(path);
		    medlib.setNmtRootPath(nmtpath);
		    medlib.setExcludes(excludes);
		    movieLibraryPaths.add(medlib);
			    
		    logger.fine("Found media library: " + medlib);
		}
	}
}