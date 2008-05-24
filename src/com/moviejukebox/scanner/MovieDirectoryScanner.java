package com.moviejukebox.scanner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;

/**
 * DirectoryScanner. Build a library with files with extension 
 * AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG other files are excluded 
 * from the library
 * 
 * @author jjulien
 */
public class MovieDirectoryScanner {
	
	protected int mediaLibraryRootPathIndex;
	private String mediaLibraryRoot;
	
	/**
	 * Scan the specified directory for movies files. 
	 * @param directory movie library rootfile
	 * @return a new library
	 */
	public Library scan(File directory) {
		
        if (directory.isFile())
            mediaLibraryRoot = directory.getParentFile().getAbsolutePath();
        else {
            mediaLibraryRoot = directory.getAbsolutePath();
        }
    
        mediaLibraryRootPathIndex = mediaLibraryRoot.length();
		
        Library library = new Library();
		this.scanDirectory(directory, library);
		return library;
	}

	protected void scanDirectory(File directory, Library collection) {
		if (directory.isFile())
			scanFile(directory, collection);
		else {
			List<File> files = Arrays.asList(directory.listFiles());
			Collections.sort(files);
			
			for (File file : files) {
				if (file.isDirectory()) {
					scanDirectory(file, collection);
				} else {
					scanFile(file, collection);
				}
			}
		}		
	}
	
	protected void scanFile(File file, Library library) {
		String filename = file.getName();
		int index = filename.lastIndexOf(".");
		if (index < 0) return;
		
		String extension = file.getName().substring(index+1).toUpperCase();
		if ("AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG".indexOf(extension) >= 0) {

			String relativeFilename = file.getAbsolutePath().substring(mediaLibraryRootPathIndex);
			
			if ( relativeFilename.startsWith(File.separator) ) {
				 relativeFilename = relativeFilename.substring(1); 
			}
			
			MovieFile movieFile = new MovieFile();
			relativeFilename = relativeFilename.replace('\\', '/'); // make it unix!
			movieFile.setFilename(relativeFilename);
			movieFile.setPart(1);
						
			Movie m = new Movie();
			m.addMovieFile(movieFile);
			m.setFile(file);
			m.setBaseName(filename.substring(0, index));
			
			MovieFilenameScanner filenameScanner = new MovieFilenameScanner();
			filenameScanner.scan(m);
			
			library.addMovie(m);
	    }
	}	
}