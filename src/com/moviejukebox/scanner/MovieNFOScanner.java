package com.moviejukebox.scanner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.tools.PropertiesUtil;

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
	 * @param movieDB
	 */
	public void scan(Movie movie, MovieDatabasePlugin movieDB) {
		String fn = movie.getFile().getAbsolutePath();
//		String localMovieName = movie.getTitle();
		String localMovieDir = fn.substring(0, fn.lastIndexOf(File.separator));	// the full directory that the video file is in
		String localDirectoryName = localMovieDir.substring(localMovieDir.lastIndexOf(File.separator) + 1);	// just the sub-directory the video file is in
		String checkedFN = "";
        String NFOdirectory = PropertiesUtil.getProperty("filename.nfo.directory", "");

		// If "fn" is a file then strip the extension from the file.
		if (movie.getFile().isFile()) {
			fn = fn.substring(0, fn.lastIndexOf("."));
		} else {
            // *** First step is to check for VIDEO_TS
			// The movie is a directory, which indicates that this is a VIDEO_TS file
			// So, we should search for the file moviename.nfo in the sub-directory
			checkedFN = checkNFO(fn + fn.substring(fn.lastIndexOf(File.separator)));
		}
		
		if (checkedFN.equals("")) {
			// Not a VIDEO_TS directory so search for the variations on the filename.nfo
			// *** Second step is to check for a directory wide NFO file.
			// This file should be named the same as the directory that it is in
			// E.G. C:\TV\Chuck\Season 1\Season 1.nfo
			checkedFN = checkNFO(localMovieDir + File.separator + localDirectoryName);
			
			if (checkedFN.equals("")) {
				// *** Third step is to check for the filename.nfo dile
				// This file should be named exactly the same as the video file with an extension of "nfo" or "NFO"
				// E.G. C:\Movies\Bladerunner.720p.avi => Bladerunner.720p.nfo
				checkedFN = checkNFO(fn);
			}
            
            if (checkedFN.equals("") && !NFOdirectory.equals("")) {
                // *** Last step if we still haven't found the nfo file is to
                // search the NFO directory as specified in the moviejukebox,properties file
                checkedFN = checkNFO(movie.getLibraryPath() + NFOdirectory + File.separator + movie.getBaseName());
            }
		}

		File nfoFile = new File(checkedFN);
		
		if (nfoFile.exists()) {
			logger.finest("Scanning NFO file for Infos : " + nfoFile.getName());
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

				movieDB.scanNFO(nfo, movie);

				logger.finest("Scanning NFO for Poster URL");
				int urlStartIndex = 0;
				while (urlStartIndex >= 0 && urlStartIndex < nfo.length()) {
//					logger.finest("Looking for URL start found in nfo urlStartIndex  = " + urlStartIndex);
					int currentUrlStartIndex = nfo.indexOf("http://", urlStartIndex);
					if (currentUrlStartIndex >= 0) {
//						logger.finest("URL start found in nfo at pos=" + currentUrlStartIndex);
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
					logger.severe("Failed closing: " + e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Check to see if the passed filename exists with nfo extensions
	 * 
	 * @param checkNFOfilename (NO EXTENSION)
	 * @return blank string if not found, filename if found
	 */
    private String checkNFO(String checkNFOfilename) {
		File nfoFile = new File(checkNFOfilename + ".nfo");
		if (nfoFile.exists()) {
			return (checkNFOfilename + ".nfo");
		} else {
			nfoFile = new File(checkNFOfilename + ".NFO");
			if (nfoFile.exists()) {
				return (checkNFOfilename + ".NFO");
			} else {
				return ("");
			}
		}
	}
}
