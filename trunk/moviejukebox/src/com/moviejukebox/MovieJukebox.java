package com.moviejukebox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
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
import com.moviejukebox.plugin.DefaultPosterPlugin;
import com.moviejukebox.plugin.DefaultThumbnailPlugin;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.plugin.MovieImagePlugin;
import com.moviejukebox.scanner.MediaInfoScanner;
import com.moviejukebox.scanner.MovieDirectoryScanner;
import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.GraphicTools;
import com.moviejukebox.writer.MovieJukeboxHTMLWriter;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;
import java.security.PrivilegedAction;

public class MovieJukebox {

	private static Logger logger = Logger.getLogger("moviejukebox");

	Collection<MediaLibraryPath> movieLibraryPaths;

	private String movieLibraryRoot;
	private String jukeboxRoot;
	private String skinHome;
	private String detailsDirName;
	private boolean forceThumbnailOverwrite;
        private boolean forcePosterOverwrite;
	private Properties props;

	public static void main(String[] args) throws XMLStreamException, SecurityException, IOException, ClassNotFoundException {
		// Send logger output to our FileHandler.

		Formatter mjbFormatter = new Formatter() {
			public synchronized String format(LogRecord record) {
				return record.getMessage() + (String)java.security.AccessController.doPrivileged(
                                    new PrivilegedAction<Object>() {
                                        public Object run() {
                                            return System.getProperty("line.separator");
                                        }
                                    }
                                );
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

					File f = new File(movieLibraryRoot);
					if (f.exists() && f.isDirectory() && jukeboxRoot == null) {
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

		if (jukeboxRoot == null) {
			System.out.println("Wrong arguments specified: you must define the jukeboxRoot property (-o) !");
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
		System.out.println("");
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("Generates an HTML library for your movies library.");
		System.out.println("");
		System.out.println("MOVIELIB movieLibraryRoot [-o jukeboxRoot]");
		System.out.println("");
		System.out.println("    movieLibraryRoot    : MANDATORY");
		System.out.println("                          This parameter can be either: ");
		System.out.println("                          - An existing directory (local or network)");
		System.out.println("                            This is where your movie files are stored.");
		System.out.println("                            In this case -o is optional.");
		System.out.println("");
		System.out.println("                          - Or an XML configuration file specifying one or");
		System.out.println("                            many directories to be scanned for movies.");
		System.out.println("                            In this case -o option is MANDATORY.");
		System.out.println("                            Please check README.TXT for further information.");
		System.out.println("");
		System.out.println("    -o jukeboxRoot      : OPTIONAL (when not using XML libraries file)");
		System.out.println("                          output directory (local or network directory)");
		System.out.println("                          This is where the jukebox file will be written to");
		System.out.println("                          by default the is the same as the movieLibraryRoot");
	}

	private MovieJukebox(String source, String jukeboxRoot) {
		// Load moviejukebox.properties form the classpath
		props = new java.util.Properties();
		InputStream propertiesStream = ClassLoader.getSystemResourceAsStream("moviejukebox.properties");

		try {
			if (propertiesStream == null) {
				propertiesStream = new FileInputStream("moviejukebox.properties");
			}

			props.load(propertiesStream);
		} catch (Exception e) {
			logger.severe("Failed loading file moviejukebox.properties: Please check your configuration. The moviejukebox.properties should be in the classpath.");
		}
		
		logger.finer(props.toString());

		this.skinHome = props.getProperty("mjb.skin.dir", "./skins/default");

		try {
			propertiesStream = new FileInputStream(this.skinHome + File.separator + "skin.properties");
			props.load(propertiesStream);
		} catch (Exception e) {
			logger.severe("Failed loading file skin.properties: Please check your configuration. The moviejukebox.properties should be in the classpath, and define a property called mjb.skin.dir which point to the skin directory.");
		}
		
		
		this.movieLibraryRoot = source;
		this.jukeboxRoot = jukeboxRoot;
		this.detailsDirName = props.getProperty("mjb.detailsDirName", "Jukebox");
		this.forceThumbnailOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceThumbnailsOverwrite", "false"));
                this.forcePosterOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forcePostersOverwrite", "false"));

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

	private void generateLibrary() throws FileNotFoundException, XMLStreamException, ClassNotFoundException {
		MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter(props);
		MovieJukeboxHTMLWriter htmlWriter = new MovieJukeboxHTMLWriter(props);

		MovieDatabasePlugin movieDBPlugin = this.getMovieDatabasePlugin(props.getProperty("mjb.internet.plugin", "com.moviejukebox.plugin.ImdbPlugin"));
		MovieImagePlugin thumbnailPlugin = this.getThumbnailPlugin(props.getProperty("mjb.thumbnail.plugin", "com.moviejukebox.plugin.DefaultThumbnailPlugin"));
                MovieImagePlugin posterPlugin = this.getPosterPlugin(props.getProperty("mjb.poster.plugin", "com.moviejukebox.plugin.DefaultPosterPlugin"));

		MovieDirectoryScanner mds = new MovieDirectoryScanner(props);
		MovieNFOScanner nfoScanner = new MovieNFOScanner();
		MediaInfoScanner miScanner = new MediaInfoScanner(props);
		Movie.setup(props);

		File mediaLibraryRoot = new File(movieLibraryRoot);
		String jukeboxDetailsRoot = jukeboxRoot + File.separator + detailsDirName;

		
		//////////////////////////////////////////////////////////////////
		/// PASS 1 : Scan movie libraries for files...
		//
		logger.fine("Scanning movies directory " + mediaLibraryRoot);
		logger.fine("Jukebox output goes to " + jukeboxRoot);


		Library library = new Library();
		for (MediaLibraryPath mediaLibraryPath : movieLibraryPaths) {
			logger.finer("Scanning media library " + mediaLibraryPath.getPath());
			library = mds.scan(mediaLibraryPath, library);
		}
		
		logger.fine("Found " + library.size() + " movies in your media library");

		
		
		//////////////////////////////////////////////////////////////////
		/// PASS 2 : Scan movie libraries for files...
		//
		logger.fine("Searching for movies information...");

		for (Movie movie : library.values()) {
			// First get movie data (title, year, director, genre, etc...)
			logger.fine("Updating data for: " + movie.getTitle());
			updateMovieData(xmlWriter, movieDBPlugin, nfoScanner, miScanner, jukeboxDetailsRoot, movie);

			// Then get this movie's poster
			logger.finer("Updating poster for: " + movie.getTitle() + "...");
			updateMoviePoster(movieDBPlugin, jukeboxDetailsRoot, movie);
		}


		
		
		//////////////////////////////////////////////////////////////////
		/// PASS 3 : Indexing the library
		//
		logger.fine("Indexing libraries...");
		library.buildIndex();

		for (Movie movie : library.values()) {
			// Update movie XML files with computed index information
			logger.finest("Writing index data to movie: " + movie.getBaseName());
			xmlWriter.writeMovieXML(jukeboxDetailsRoot, movie);
			
			// Create a thumbnail for each movie
			logger.finest("Creating thumbnails for movie: " + movie.getBaseName());
			createThumbnail(thumbnailPlugin, jukeboxDetailsRoot, skinHome, movie, forceThumbnailOverwrite);
                        
                        // Create a detail poster for each movie
			logger.finest("Creating detail poster for movie: " + movie.getBaseName());
			createPoster(posterPlugin, jukeboxDetailsRoot, skinHome, movie, forcePosterOverwrite);
			
			// write the movie details HTML		
			htmlWriter.generateMovieDetailsHTML(jukeboxDetailsRoot, movie);
		}

		logger.fine("Generating Indexes...");
		xmlWriter.writeIndexXML(jukeboxDetailsRoot, detailsDirName, library);
		xmlWriter.writeCategoryXML(jukeboxRoot, detailsDirName, library);
		htmlWriter.generateMoviesIndexHTML(jukeboxRoot, detailsDirName, library);
		htmlWriter.generateMoviesCategoryHTML(jukeboxRoot, detailsDirName, library);

		logger.fine("Copying resources to Jukebox directory...");
		FileTools.copyDir(skinHome + File.separator + "html", jukeboxDetailsRoot);

		logger.fine("Process terminated.");
	}

	/**
	 * Generates a movie XML file which contains data in the <tt>Movie</tt> bean.
	 * 
	 * When an XML file exists for the specified movie file, it is loaded into the 
	 * specified <tt>Movie</tt> object.
	 * 
	 * When no XML file exist, scanners are called in turn, in order to add information
	 * to the specified <tt>movie</tt> object. Once scanned, the <tt>movie</tt> object
	 * is persisted.
	 */
	private void updateMovieData(MovieJukeboxXMLWriter xmlWriter, MovieDatabasePlugin movieDB, 
			MovieNFOScanner nfoScanner, MediaInfoScanner miScanner, 
			String jukeboxDetailsRoot, Movie movie) throws FileNotFoundException, XMLStreamException {
		
		// For each movie in the library, if an XML file for this
		// movie already exist, then no need to search for movie
		// information, just parse the XML data.
		File xmlFile = new File(jukeboxDetailsRoot + File.separator + movie.getBaseName() + ".xml");
		
		if (xmlFile.exists()) {
			// parse the movie XML file
			logger.finer("movie XML file found for movie:" + movie.getBaseName());
			xmlWriter.parseMovieXML(xmlFile, movie);

			// Update thumbnails format if needed
			String thumbnailExtension = props.getProperty("thumbnails.format", "png");
			movie.setThumbnailFilename(movie.getBaseName() + "_small." + thumbnailExtension);
                        // Update poster format if needed
			String posterExtension = props.getProperty("posters.format", "png");
			movie.setDetailPosterFilename(movie.getBaseName() + "_large." + posterExtension);
			
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
	}

	/**
	 * Update the movie poster for the specified movie.
	 * 
	 * When an existing thumbnail is found for the movie, it is not overwriten,
	 * unless the mjb.forceThumbnailOverwrite is set to true in the property file.
	 * 
	 * When the specified movie does not contain a valid URL for the poster, a 
	 * dummy image is used instead.
	 */
	private void updateMoviePoster(MovieDatabasePlugin movieDB, String jukeboxDetailsRoot, Movie movie) {
		String posterFilename = jukeboxDetailsRoot + File.separator + movie.getPosterFilename();
		File posterFile = new File(posterFilename);

		// Do not overwrite existing posters
		if (!posterFile.exists()) {
			posterFile.getParentFile().mkdirs();

			if (movie.getPosterURL() == null || movie.getPosterURL().equalsIgnoreCase("Unknown")) {
				logger.finest("Dummy image used for " + movie.getBaseName());
				FileTools.copyFile(
					new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"),
					new File(jukeboxDetailsRoot + File.separator + movie.getPosterFilename()));				
			} else {
				try {
					logger.finest("Downloading poster for " + movie.getBaseName() + " [calling plugin]");
					downloadPoster(posterFile, movie);
				} catch (Exception e) {
					logger.finer("Failed downloading movie poster : " + movie.getPosterURL());
					FileTools.copyFile(
							new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"),
							new File(jukeboxDetailsRoot + File.separator + movie.getPosterFilename()));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<MediaLibraryPath> parseMovieLibraryRootFile(File f) {
		Collection<MediaLibraryPath> movieLibraryPaths = new ArrayList<MediaLibraryPath>();

		if (!f.exists() || f.isDirectory()) {
			logger.severe("The moviejukebox library input file you specified is invalid: " + f.getName());
			return movieLibraryPaths;
		}

		try {
			XMLConfiguration c = new XMLConfiguration(f);

			List<HierarchicalConfiguration> fields = c.configurationsAt("library");
			for (Iterator<HierarchicalConfiguration> it = fields.iterator(); it.hasNext();) {
				HierarchicalConfiguration sub = it.next();
				// sub contains now all data about a single medialibrary node
				String path = sub.getString("path");
				String nmtpath = sub.getString("nmtpath");
				List<String> excludes = sub.getList("exclude[@name]");

				if (new File(path).exists()) {
					MediaLibraryPath medlib = new MediaLibraryPath();
					medlib.setPath(path);
					medlib.setNmtRootPath(nmtpath);
					medlib.setExcludes(excludes);
					movieLibraryPaths.add(medlib);
					logger.fine("Found media library: " + medlib);
				} else {
					logger.fine("Skipped invalid media library: " + path);
				}
			}
		} catch (Exception e) {
			logger.severe("Failed parsing moviejukebox library input file: " + f.getName());
			e.printStackTrace();
		}
		return movieLibraryPaths;
	}

	public MovieDatabasePlugin getMovieDatabasePlugin(String className) {
		MovieDatabasePlugin movieDB;

		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<? extends MovieDatabasePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieDatabasePlugin.class);
			movieDB = pluginClass.newInstance();
		} catch (Exception e) {
			movieDB = new ImdbPlugin();
			logger.severe("Failed instanciating MovieDatabasePlugin: " + className);
			logger.severe("Default IMDb plugin will be used instead.");
			e.printStackTrace();
		}

		movieDB.init(props);
		return movieDB;
	}
	
	public MovieImagePlugin getThumbnailPlugin(String className) {
		MovieImagePlugin thumbnailPlugin;

		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class< ?extends DefaultThumbnailPlugin> pluginClass = cl.loadClass(className).asSubclass(DefaultThumbnailPlugin.class);
			thumbnailPlugin = pluginClass.newInstance();
		} catch (Exception e) {
			thumbnailPlugin = new DefaultThumbnailPlugin();
			logger.severe("Failed instanciating ThumbnailPlugin: " + className);
			logger.severe("Default thumbnail plugin will be used instead.");
			e.printStackTrace();
		}

		thumbnailPlugin.init(props);
		return thumbnailPlugin;
	}

	public MovieImagePlugin getPosterPlugin(String className) {
		MovieImagePlugin posterPlugin;

		try {
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class< ?extends MovieImagePlugin> pluginClass = cl.loadClass(className).asSubclass(MovieImagePlugin.class);
			posterPlugin = pluginClass.newInstance();
		} catch (Exception e) {
			posterPlugin = new DefaultPosterPlugin();
			logger.severe("Failed instanciating PosterPlugin: " + className);
			logger.severe("Default poster plugin will be used instead.");
			e.printStackTrace();
		}

		posterPlugin.init(props);
		return posterPlugin;
	}
        
	/**
	 * Download the movie poster for the specified movie into the specified file.
	 * @throws IOException
	 */
	public static void downloadPoster(File posterFile, Movie mediaFile) throws IOException {
		URL url = new URL(mediaFile.getPosterURL());
		URLConnection cnx = url.openConnection();
	
		// Let's pretend we're Firefox...
		cnx.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-GB; rv:1.8.1.5) Gecko/20070719 Iceweasel/2.0.0.5 (Debian-2.0.0.5-0etch1)");

		FileTools.copy(cnx.getInputStream(), new FileOutputStream(posterFile));
	}

	public static void createThumbnail(MovieImagePlugin thumbnailManager, String rootPath, String skinHome, Movie movie, boolean forceThumbnailOverwrite) {
		try {
			String src = rootPath + File.separator + movie.getPosterFilename();
			String dst = rootPath + File.separator + movie.getThumbnailFilename();
	
			if (!(new File(dst).exists()) || forceThumbnailOverwrite) {
				FileInputStream fis = new FileInputStream(src);
				BufferedImage bi = GraphicTools.loadJPEGImage(fis);
				if (bi == null) {
                                        logger.info("Using dummy thumbnail image for " + movie.getTitle());
					FileTools.copyFile(
							new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"),
							new File(rootPath + File.separator + movie.getPosterFilename()));
					fis = new FileInputStream(src);
					bi = GraphicTools.loadJPEGImage(fis);
				}
				
				bi = thumbnailManager.generate(movie, bi);
				
				GraphicTools.saveImageToDisk(bi, dst);
			}
		} catch (Exception e) {
			logger.severe("Failed creating thumbnail for " + movie.getTitle());
			e.printStackTrace();
		}
	}
        
	public static void createPoster(MovieImagePlugin posterManager, String rootPath, String skinHome, Movie movie, boolean forcePosterOverwrite) {
		try {
			String src = rootPath + File.separator + movie.getPosterFilename();
			String dst = rootPath + File.separator + movie.getDetailPosterFilename();
	
			if (!(new File(dst).exists()) || forcePosterOverwrite) {
				FileInputStream fis = new FileInputStream(src);
				BufferedImage bi = GraphicTools.loadJPEGImage(fis);
				if (bi == null) {
                                        logger.info("Using dummy poster image for " + movie.getTitle());
					FileTools.copyFile(
							new File(skinHome + File.separator + "resources" + File.separator + "dummy.jpg"),
							new File(rootPath + File.separator + movie.getPosterFilename()));
					fis = new FileInputStream(src);
					bi = GraphicTools.loadJPEGImage(fis);
				}
				
				bi = posterManager.generate(movie, bi);
				
				GraphicTools.saveImageToDisk(bi, dst);
			}
		} catch (Exception e) {
			logger.severe("Failed creating poster for " + movie.getTitle());
			e.printStackTrace();
		}
	}
        
}
