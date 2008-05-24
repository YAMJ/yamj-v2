package com.moviejukebox;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLStreamException;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieDatabasePlugin;
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

	public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
		
		String movieLibraryRoot = null;
		String jukeboxRoot = null;
		String detailsDirName = "Jukebox";
		String nmtRootPath = "file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/";
		boolean forceXMLOverwrite = false;
		boolean forceHTMLOverwrite = false;
		int thumbWidth = 140;
		int thumbHeight = 200;

		if (args.length==0) {
			help();
			return;
		} 
		
		try {
			for (int i = 0; i < args.length; i++) {
				String arg = (String) args[i];
				if ("-o".equalsIgnoreCase(arg)) {
					jukeboxRoot = args[++i]; 
				} else if ("-d".equalsIgnoreCase(arg)) {
					detailsDirName = args[++i]; 
				} else if ("-nr".equalsIgnoreCase(arg)) {
					nmtRootPath = args[++i]; 
				} else if ("-tw".equalsIgnoreCase(arg)) {
					try {
						thumbWidth = Integer.parseInt(args[++i]); 
					} catch (NumberFormatException e) {
						thumbWidth = 140;
					}
				} else if ("-th".equalsIgnoreCase(arg)) {
					try {
						thumbHeight = Integer.parseInt(args[++i]); 
					} catch (NumberFormatException e) {
						thumbHeight = 200;
					}
				} else if ("-fx".equalsIgnoreCase(arg)) {
					forceXMLOverwrite = true;
				} else if ("-fh".equalsIgnoreCase(arg)) {
					forceHTMLOverwrite = true;
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
		
		MovieJukebox ml = new MovieJukebox(movieLibraryRoot, jukeboxRoot, detailsDirName, 
				forceXMLOverwrite, forceHTMLOverwrite, nmtRootPath, thumbWidth, thumbHeight);
		
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
		System.out.println("    -d detailsDirName   : OPTIONAL");
		System.out.println("                          name of the details directory of the jukebox");
		System.out.println("                          Default is: \"Jukebox\"");
		System.out.println("");
		System.out.println("    -nr nmtRootPath     : OPTIONAL");
		System.out.println("                          path of the media library root on your NMT");
		System.out.println("                          Default is: \"file:///opt/sybhttpd/localhost.drives/HARD_DISK/Video/\"");
		System.out.println("");
		System.out.println("    -tw                 : OPTIONAL");
		System.out.println("                          Generated thumbnails width ");
		System.out.println("                          Default is: 140");
		System.out.println("");
		System.out.println("    -th                 : OPTIONAL");
		System.out.println("                          Generated thumbnails height");
		System.out.println("                          Default is: 200");
		System.out.println("");
		System.out.println("    -fx                 : OPTIONAL");
		System.out.println("                          force the jukeboxe's XML files to be overwritten");
		System.out.println("                          Default is: false");
		System.out.println("");
		System.out.println("    -fh                 : OPTIONAL");
		System.out.println("                          force the jukeboxe's HTML files");
		System.out.println("                          Default is: false");
	}

	private MovieJukebox(String movieLibraryRoot, String jukeboxRoot, String detailsDirName, 
			boolean forceXMLOverwrite, boolean forceHTMLOverwrite, String nmtRootPath, int thumbWidth, int thumbHeight) {
		this.movieLibraryRoot = movieLibraryRoot;
		this.jukeboxRoot = jukeboxRoot;
		this.detailsDirName = detailsDirName;
		this.forceXMLOverwrite = forceXMLOverwrite;
		this.forceHTMLOverwrite = forceHTMLOverwrite;
		this.nmtRootPath = nmtRootPath;
		this.thumbWidth = thumbWidth;
		this.thumbHeight = thumbHeight;
		
		System.out.println("Starting MovieJukebox v1.0.5 beta");
		System.out.println("[movieLibraryRoot="+ movieLibraryRoot+"]");
		System.out.println("[jukeboxRoot="+ jukeboxRoot+"]");
		System.out.println("[detailsDirName="+ detailsDirName+"]");
		System.out.println("[forceXMLOverwrite="+ forceXMLOverwrite+"]");
		System.out.println("[forceHTMLOverwrite="+ forceHTMLOverwrite+"]");
		System.out.println("[nmtRootPath="+ nmtRootPath+"]");
		System.out.println("[thumbWidth="+ thumbWidth+"]");
		System.out.println("[thumbHeight="+ thumbHeight+"]");
		System.out.println("");
	}

	private void generateLibrary() throws FileNotFoundException, XMLStreamException {
		MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter(nmtRootPath, forceXMLOverwrite);
		MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter(forceHTMLOverwrite);
		MovieDatabasePlugin movieDB = new ImdbPlugin();
		MovieDirectoryScanner mds = new MovieDirectoryScanner();
		MovieNFOScanner nfoScanner = new MovieNFOScanner();

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
