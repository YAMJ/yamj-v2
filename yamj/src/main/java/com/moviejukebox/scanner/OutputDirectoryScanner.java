/*
 *      Copyright (c) 2004-2012 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *
 *      For any reuse or distribution, you must make clear to others the
 *      license terms of this work.
 */
package com.moviejukebox.scanner;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFileNameDTO;
import com.moviejukebox.reader.MovieJukeboxXMLReader;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.StringTools;
import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class OutputDirectoryScanner {

    private static final Logger logger = Logger.getLogger(OutputDirectoryScanner.class);
    private static final String LOG_MESSAGE = "OutputDirectoryScanner: ";
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
        logger.debug(LOG_MESSAGE + "Scanning " + scanDir);
        File scanDirFile = new FileTools.FileEx(scanDir);
        if (null != scanDirFile) {

            if (scanDirFile.isDirectory()) {
                MovieJukeboxXMLReader xmlReader = new MovieJukeboxXMLReader();

                for (File file : scanDirFile.listFiles()) {

                    String filename = file.getName();

                    if (filename.length() > 4 && FilenameUtils.getExtension(filename).equalsIgnoreCase("xml")) {
                        FileTools.fileCache.fileAdd(file);
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

                        if (skip) {
                            continue;
                        }

                        logger.debug(LOG_MESSAGE + "  Found XML file: " + filename);

                        Movie movie = new Movie();
                        /*
                         *  Because the XML can have more info available than the original filename did,
                         *  the usual key construction method is not stable across runs. So we have to find
                         *  what the key *would* have been, if all we knew about the movie was the filename.
                         */
                        MovieFileNameDTO dto = MovieFilenameScanner.scan(file);
                        movie.mergeFileNameDTO(dto);
                        String key = Library.getMovieKey(movie);

                        if (library.containsKey(key)) {
                            logger.debug(LOG_MESSAGE + "  Video already in library: " + key);
                        } else {
                            if (xmlReader.parseMovieXML(file, movie) && StringTools.isValidString(movie.getBaseName())) {
                                logger.debug(LOG_MESSAGE + "  Parsed movie: " + movie.getTitle());

                                if (!library.containsKey(Library.getMovieKey(movie))) {
                                    logger.debug(LOG_MESSAGE + "  Adding unscanned video " + Library.getMovieKey(movie));
                                    movie.setFile(file);
                                    library.addMovie(key, movie);
                                }

                            } else {
                                logger.debug(LOG_MESSAGE + "  Invalid video XML file");
                            }
                        }
                    } else {
                        logger.debug(LOG_MESSAGE + "  Skipping file: " + filename);
                    }
                }
            } else {
                logger.debug(LOG_MESSAGE + "  Specified path is not a directory: " + scanDir);
            }
        }
    }
}