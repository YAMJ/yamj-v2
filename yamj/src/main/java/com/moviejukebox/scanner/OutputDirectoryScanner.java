/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputDirectoryScanner {

    private static final Logger LOG = LoggerFactory.getLogger(OutputDirectoryScanner.class);
    private final String scanDir;

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
        LOG.debug("Scanning {}", scanDir);
        File scanDirFile = new FileTools.FileEx(scanDir);
        if (scanDirFile != null) {

            if (scanDirFile.isDirectory()) {
                MovieJukeboxXMLReader xmlReader = new MovieJukeboxXMLReader();

                for (File file : scanDirFile.listFiles()) {

                    String filename = file.getName();

                    if (filename.length() > 4 && "xml".equalsIgnoreCase(FilenameUtils.getExtension(filename))) {
                        FileTools.fileCache.fileAdd(file);
                        String filenameUpper = filename.toUpperCase();
                        boolean skip = "CATEGORIES.XML".equals(filenameUpper);
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

                        LOG.debug("  Found XML file: {}", filename);

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
                            LOG.debug("  Video already in library: {}", key);
                        } else {
                            if (xmlReader.parseMovieXML(file, movie) && StringTools.isValidString(movie.getBaseName())) {
                                LOG.debug("  Parsed movie: {}", movie.getTitle());

                                if (!library.containsKey(Library.getMovieKey(movie))) {
                                    LOG.debug("  Adding unscanned video {}", Library.getMovieKey(movie));
                                    movie.setFile(file);
                                    library.addMovie(key, movie);
                                }

                            } else {
                                LOG.debug("  Invalid video XML file");
                            }
                        }
                    } else {
                        LOG.debug("  Skipping file: {}", filename);
                    }
                }
            } else {
                LOG.debug("  Specified path is not a directory: {}", scanDir);
            }
        }
    }
}
