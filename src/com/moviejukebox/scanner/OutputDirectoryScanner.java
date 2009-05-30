package com.moviejukebox.scanner;

import java.io.File;
import java.util.logging.Logger;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFileNameDTO;
import com.moviejukebox.writer.MovieJukeboxXMLWriter;

public class OutputDirectoryScanner {
    private static Logger logger = Logger.getLogger("moviejukebox");
    
    private String scanDir;
    
    public OutputDirectoryScanner(String scanDir) {
        this.scanDir = scanDir;
    }
    
    public void scan(Library library) {
    /*
        Map<String, Movie> xmlLibrary = new HashMap<String, Movie>();
        scanXMLFiles(xmlLibrary);
        
        // Because the XML can have additional info, the key is not stable between rns
    
    protected void scanXMLFiles(Map<String, Movie> library) {
    */
        logger.finer("OutputDirectoryScanner: scanning " + scanDir);
        File scanDirFile = new File(scanDir);
        if (null != scanDirFile) {
        
            if (scanDirFile.isDirectory()) {
                MovieJukeboxXMLWriter xmlWriter = new MovieJukeboxXMLWriter();
                
                for (File file : scanDirFile.listFiles()) {
                
                    String filename = file.getName();
                    
                    if (filename.length() > 4 && ".xml".equalsIgnoreCase(filename.substring(filename.length() - 4))) {
                        String filenameUpper = filename.toUpperCase();
                        boolean skip = filenameUpper.equals("CATEGORIES.XML");
                        if (!skip) {
                            for (String prefix : Library.getPrefixes()) {
                                if (filenameUpper.startsWith(prefix + "_")) {
                                    skip = true;
                                    break;
                                }
                            }
                        }
                        
                        if (skip) continue;
                    
                        logger.finest("  Found XML file: " + filename);
                        
                        Movie movie = new Movie();
                        // Because the XML can have more info available than the original filename did,
                        // the usual key construction method is not stable across runs. So we have to find
                        // what the key *would* have been, if all we knew about the movie was the filename.
                        MovieFileNameDTO dto = MovieFilenameScanner.scan(file);
                        movie.mergeFileNameDTO(dto);
                        String key = Library.getMovieKey(movie);
                        
                        if (!library.containsKey(key)) {
                            if (xmlWriter.parseMovieXML(file, movie) && movie.getBaseName() != null) {
                                logger.finest("  Parsed movie: " + movie.getTitle());
                                
                                if (!library.containsKey(Library.getMovieKey(movie))) {
                                    logger.finer("  Adding unscanned movie " + Library.getMovieKey(movie));
                                    movie.setFile(file);
                                    library.addMovie(key, movie);
                                }
                                
                            } else {
                                logger.finest("  Failed parsing movie");
                            }
                            
                        } else {
                            logger.finest("  Movie already in library: " + key);
                        }
                        
                    } else {
                        logger.finest("  Skipping file: " + filename);
                    }
                }
            } else {
                logger.finer("  Specified path is not a directory: " + scanDir);
            }
        }
    }
}