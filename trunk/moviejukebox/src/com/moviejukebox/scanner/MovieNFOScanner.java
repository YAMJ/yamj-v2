package com.moviejukebox.scanner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.plugin.ImdbPlugin;

/**
 * NFO file parser.
 * 
 * Search a NFO file for IMDb URL.
 * 
 * @author jjulien
 */
public class MovieNFOScanner {

	private static Logger logger = Logger.getLogger("moviejukebox");

	static final int BUFF_SIZE = 100000;

	static final byte[] buffer = new byte[BUFF_SIZE];

	/**
	 * Search the IMDBb id of the specified movie in the NFO file if it exists.
	 * 
	 * @param movie
	 *            movie to scan
	 */
	public void scan(Movie movie) {
		Properties props = new Properties();
		InputStream propertiesStream = ClassLoader.getSystemResourceAsStream("moviejukebox.properties");

		try {
			if (propertiesStream == null) {
				propertiesStream = new FileInputStream("moviejukebox.properties");
			}

			props.load(propertiesStream);
		} catch (Exception e) {
			logger
					.severe("Failed loading file moviejukebox.properties: Please check your configuration. The moviejukebox.properties should be in the classpath.");
		}

		logger.finer(props.toString());

		String fn = movie.getFile().getAbsolutePath();
		if (movie.getFile().isFile()) {
			int i = fn.lastIndexOf(".");
			fn = fn.substring(0, i);
		}
		File nfoFile = new File(fn + ".nfo");

		if (!nfoFile.exists()) {
			nfoFile = new File(fn + ".NFO");
		}

		if (nfoFile.exists()) {

			InputStream in = null;
			ByteArrayOutputStream out = null;
			try {
				in = new FileInputStream(nfoFile);
				out = new ByteArrayOutputStream();
				while (true) {
					synchronized (buffer) {
						int amountRead = in.read(buffer);
						if (amountRead == -1) {
							break;
						}
						out.write(buffer, 0, amountRead);
					}
				}

				String nfo = out.toString();

				String movieDatabasePlugin = props.getProperty("mjb.internet.plugin",
						"com.moviejukebox.plugin.ImdbPlugin");
				// Always look for imdb id look for ttXXXXXX
				int beginIndex = nfo.indexOf("/tt");
				if (beginIndex != -1) {
					StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1),
							"/ \n,:!&é\"'(--è_çà)=$");
					movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, st.nextToken());
				}
				if (movieDatabasePlugin.equals("com.moviejukebox.plugin.AllocinePlugin")) {
					// If we use allocine plugin look for
					// http://www.allocine.fr/...=XXXXX.html
					logger.finest("Scanning NFO for Allocine Id");
					beginIndex = nfo.indexOf("http://www.allocine.fr/");
					if (beginIndex != -1) {
						int beginIdIndex = nfo.indexOf("=", beginIndex);
						if (beginIdIndex != -1) {
							int endIdIndex = nfo.indexOf(".", beginIdIndex);
							if (endIdIndex != -1) {
								logger.finer("Allocine Id found in nfo = "
										+ nfo.substring(beginIdIndex + 1, endIdIndex));
								movie.setId(AllocinePlugin.ALLOCINE_PLUGIN_ID, nfo.substring(beginIdIndex + 1,
										endIdIndex));
							}
						}
					}
				}

				int urlStartIndex = 0;
				while (urlStartIndex >= 0 && urlStartIndex < nfo.length()) {
					logger.finest("Looking for URL start found in nfo urlStartIndex  = " + urlStartIndex);
					int currentUrlStartIndex = nfo.indexOf("http://", urlStartIndex);
					if (currentUrlStartIndex >= 0) {
						logger.finest("URL start found in nfo at pos=" + currentUrlStartIndex);
						int currentUrlEndIndex = nfo.indexOf("jpg", currentUrlStartIndex);
						if (currentUrlEndIndex < 0) {
							currentUrlEndIndex = nfo.indexOf("JPG", currentUrlStartIndex);
						}
						if (currentUrlEndIndex >= 0) {
							int nextUrlStartIndex = nfo.indexOf("http://", currentUrlStartIndex);
							// look for shortest http://
							while((nextUrlStartIndex != -1) && (nextUrlStartIndex < currentUrlEndIndex + 3)) {
								currentUrlStartIndex = nextUrlStartIndex;
								nextUrlStartIndex = nfo.indexOf("http://", currentUrlStartIndex + 1);
							}
							logger.finer("Poster URL found in nfo = "
									+ nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
							movie.setPosterURL(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
							urlStartIndex = -1;
						} else {
							urlStartIndex = currentUrlStartIndex + 3;
						}
					} else {
						urlStartIndex = -1;
					}
				}

			} catch (IOException e) {
				logger.severe("Failed reading " + nfoFile.getName());
				e.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {

				}
			}
		}
	}
}
