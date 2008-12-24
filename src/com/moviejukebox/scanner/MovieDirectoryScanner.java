package com.moviejukebox.scanner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * DirectoryScanner
 * 
 * @author jjulien
 * @author gaelead
 * @author jriihi
 */
public class MovieDirectoryScanner {
	
	protected int mediaLibraryRootPathIndex;
	private String mediaLibraryRoot;
	private String supportedExtensions;
	private String thumbnailsFormat;
    private String postersFormat;
	
	private static Logger logger = Logger.getLogger("moviejukebox");

	//BD rip infos Scanner
	private BDRipScanner localBDRipScanner;
	
	public MovieDirectoryScanner() {
            supportedExtensions = PropertiesUtil.getProperty("mjb.extensions", "AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG MOV");
            thumbnailsFormat = PropertiesUtil.getProperty("thumbnails.format", "png");
            postersFormat = PropertiesUtil.getProperty("posters.format", "png");
            
			localBDRipScanner = new BDRipScanner();
	}

	/**
	 * Scan the specified directory for movies files. 
	 * @param directory movie library rootfile
	 * @return a new library
	 */
	public Library scan(MediaLibraryPath srcPath, Library library) {
		
		File directory = new File(srcPath.getPath());
		
        if (directory.isFile())
            mediaLibraryRoot = directory.getParentFile().getAbsolutePath();
        else {
            mediaLibraryRoot = directory.getAbsolutePath();
        }
    
        mediaLibraryRootPathIndex = mediaLibraryRoot.length();
		
		this.scanDirectory(srcPath, directory, library);
		return library;
	}

	protected void scanDirectory(MediaLibraryPath srcPath, File directory, Library collection) {
		if (directory.isFile())
			scanFile(srcPath, directory, collection);
		else {
			File[] contentList = directory.listFiles();
			if (contentList!=null) {
				List<File> files = Arrays.asList(contentList);
				Collections.sort(files);

				for (File file : files) {
					if (file.isDirectory() && file.getName().equalsIgnoreCase("VIDEO_TS")) {
						scanFile(srcPath, file.getParentFile(), collection);
					} else if (file.isDirectory() && file.getName().equalsIgnoreCase("BDMV")) {
						scanFile(srcPath, file.getParentFile(), collection);
					} else if (file.isDirectory()) {
						scanDirectory(srcPath, file, collection);
					} else if ( !isFiltered(srcPath, file) ){
						scanFile(srcPath, file, collection);
					}
				}
			}
		}		
	}

	protected boolean isFiltered(MediaLibraryPath srcPath, File file) {
		String filename = file.getName();
		int index = filename.lastIndexOf(".");
		if (index < 0) 
			return true;
		
		String extension = file.getName().substring(index+1).toUpperCase();
		if (supportedExtensions.indexOf(extension) == -1) 
			return true;

		// Compute the relative filename
		String relativeFilename = file.getAbsolutePath().substring(mediaLibraryRootPathIndex);
		if ( relativeFilename.startsWith(File.separator) ) {
			 relativeFilename = relativeFilename.substring(1); 
		}
		
		for (String excluded : srcPath.getExcludes()) {
			excluded = excluded.replace("/", File.separator);
			excluded = excluded.replace("\\", File.separator);
			String relativeFileNameLower = relativeFilename.toLowerCase();
			if(relativeFileNameLower.indexOf(excluded.toLowerCase()) >= 0) {
				logger.fine("File " + filename + " excluded.");
				return true;
			}
		}
		
		return false;
	}
	
	protected void scanFile(MediaLibraryPath srcPath, File file, Library library) {
		
		File contentFile=file;
		
		if (file.isDirectory()) {
			//Scan BD M2TS files
			File bdMainMovie = localBDRipScanner.executeGetBDInfo(file);
			
			if (bdMainMovie!=null)
				contentFile=bdMainMovie;
		}

		
		// Compute the baseFilename: This is the filename with no the extension
		String baseFileName = file.getName();
		if (!file.isDirectory()) {
			baseFileName = baseFileName.substring(0, file.getName().lastIndexOf("."));
		}
		
		// Compute the relative filename
		String relativeFilename = contentFile.getAbsolutePath().substring(mediaLibraryRootPathIndex);
		if ( relativeFilename.startsWith(File.separator) ) {
			 relativeFilename = relativeFilename.substring(1); 
		}
		
		MovieFile movieFile = new MovieFile();
		relativeFilename = relativeFilename.replace('\\', '/'); // make it unix!
		
		if (contentFile.isDirectory()) {
			// For DVD images
			movieFile.setFilename(srcPath.getNmtRootPath() + relativeFilename + "/VIDEO_TS");
		} else {
			movieFile.setFilename(srcPath.getNmtRootPath() + relativeFilename);
		}
		movieFile.setPart(1);
        movieFile.setFile(contentFile);
					
		Movie m = new Movie();
		m.addMovieFile(movieFile);
		m.setFile(contentFile);
		m.setBaseName(baseFileName);
		m.setLibraryPath(srcPath.getPath());
		m.setPosterFilename(baseFileName + ".jpg");
		m.setThumbnailFilename(baseFileName + "_small."+ thumbnailsFormat);
        m.setDetailPosterFilename(baseFileName + "_large." + postersFormat);
        m.setLibraryDescription(srcPath.getDescription());
        m.setPrebuf(srcPath.getPrebuf());
        
		MovieFilenameScanner filenameScanner = new MovieFilenameScanner();
		filenameScanner.scan(m);
		
		library.addMovie(m);
	}	
}