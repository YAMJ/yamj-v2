package com.moviejukebox;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.stream.XMLStreamException;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieDatabasePlugin;

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
		boolean forceXMLOverwrite = true;
		boolean forceHTMLOverwrite = true;
		int thumbWidth = 140;
		int thumbHeight = 200;

		if (args.length==0 || args.length>6) {
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
						thumbHeight = Integer.parseInt(args[i]); 
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
		
		System.out.println("Starting Jukebox creation with parameters:");
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
		MovieJukeboxScanner ms = new MovieJukeboxScanner();
		MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter(nmtRootPath, forceXMLOverwrite);
		MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter(forceHTMLOverwrite);
		MovieDatabasePlugin movieDB = new ImdbPlugin();

		File mediaLibraryRoot = new File(movieLibraryRoot);
		String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;

		System.out.println("Scanning movies in directory " + mediaLibraryRoot);
		Library library = ms.scan(mediaLibraryRoot);
		System.out.println("Found " + library.size() + " movies in your media library");
		
		System.out.println("Searching Internet for library information...");
		// retreiving internet Data
		for (Movie movie : library.values()) {
			File xmlFile = new File(jukeboxDetailsRoot + File.separator + movie.getBaseName() + ".xml");
			if (xmlFile.exists()) {
				xmlWriter.parseMovieXML(xmlFile, movie);
			} else {
				movieDB.scan(mediaLibraryRoot.getAbsolutePath(), movie);
			}
			
			// Download poster file only if this file doesn't exist... never overwrite an existing file...
			movieDB.downloadPoster(jukeboxDetailsRoot, movie);
			MovieJukeboxTools.createThumbnail(jukeboxDetailsRoot, movie, thumbWidth, thumbHeight);
			System.out.println(movie);
		}
		
		System.out.println("Building library indexes...");
		library.buildIndex();
		
		// build reports
		System.out.println("Generating movies HTML...");
		for (Movie movie : library.values()) {
			xmlWriter.writeMovieXML(jukeboxDetailsRoot, movie);
			htmlWriter.generateMovieDetailsHTML(jukeboxDetailsRoot, movie);
		}
		
		System.out.println("Generating movies index HTML...");
		xmlWriter.writeIndexXML(jukeboxRoot, detailsDirName, library);
		htmlWriter.generateMoviesIndexHTML(jukeboxRoot, library);		
		
		MovieJukeboxTools.copyResource("exportdetails_item_popcorn.css", jukeboxDetailsRoot);
		MovieJukeboxTools.copyResource("exportindex_item_pch.css", jukeboxRoot);
		
		System.out.println("Process terminated.");
	}
}
