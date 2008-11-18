package com.moviejukebox.scanner;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.TrailerFile;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.MovieDatabasePlugin;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.XMLHelper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

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

    private static final String BOM1 = "" + (char)239 + (char)187 + (char)191;
    private static final String BOM2 = "" + (char)254 + (char)255;
    private static final String BOM3 = "" + (char)255 + (char)254;
    private static final String BOM4 = "" + (char)00 + (char)00 + (char)254 + (char)255;
    private static final String BOM5 = "" + (char)254 + (char)255 + (char)00 + (char)00;
    
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
                String sLibraryPath = movie.getLibraryPath();
                if ((sLibraryPath.lastIndexOf("\\") == sLibraryPath.length()) || (sLibraryPath.lastIndexOf("/") == sLibraryPath.length())) {
                    checkedFN = checkNFO(movie.getLibraryPath() + NFOdirectory + File.separator + movie.getBaseName());
                } else {
                    checkedFN = checkNFO(movie.getLibraryPath() + File.separator + NFOdirectory + File.separator + movie.getBaseName());
                }
            }
        }

        File nfoFile = new File(checkedFN);

        if (nfoFile.exists()) {
            logger.finest("Scanning NFO file for Infos : " + nfoFile.getName());
            BufferedReader in = null;
            StringBuffer out = new StringBuffer();
            try {
                in = new BufferedReader(new FileReader(nfoFile));
                String line = in.readLine();
                while (line != null) {
                    out.append(line);
                    line = in.readLine();
                }
                
                String nfo = out.toString();
                if (nfo.startsWith(BOM1)) {
                    nfo = nfo.substring(BOM1.length());
                } else if (nfo.startsWith(BOM2)) {
                    nfo = nfo.substring(BOM2.length());
                } else if (nfo.startsWith(BOM3)) {
                    nfo = nfo.substring(BOM3.length());
                } else if (nfo.startsWith(BOM4)) {
                    nfo = nfo.substring(BOM4.length());
                } else if (nfo.startsWith(BOM5)) {
                    nfo = nfo.substring(BOM5.length());
                }

                if (!parseXMLNFO(nfo, movie)) {
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
                                while ((nextUrlStartIndex != -1) && (nextUrlStartIndex < currentUrlEndIndex + 3)) {
                                    currentUrlStartIndex = nextUrlStartIndex;
                                    nextUrlStartIndex = nfo.indexOf("http://", currentUrlStartIndex + 1);
                                }
                                logger.finer("Poster URL found in nfo = " + nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
                                movie.setPosterURL(nfo.substring(currentUrlStartIndex, currentUrlEndIndex + 3));
                                urlStartIndex = -1;
                            } else {
                                urlStartIndex = currentUrlStartIndex + 3;
                            }
                        } else {
                            urlStartIndex = -1;
                        }
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
//        logger.finest("checkNFO = " + checkNFOfilename);
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
    
    private boolean parseXMLNFO(String nfo, Movie movie) {
        boolean retval = true;
        if (nfo.indexOf("<movie>") > -1) {
            parseMovieNFO(nfo, movie);
        //} else if (nfo.indexOf("<tvshow>") > -1) {
        //    parseTVNFO(nfo, movie);
        //} else if (nfo.indexOf("<episodedetails>") > -1) {
        //    parseEpisodeNFO(nfo, movie);
        } else {
            retval = false;
        }
        return retval;
    }
    
    /**
     * Used to parse out the XBMC nfo xml data for movies
     * 
     * @param xmlFile
     * @param movie
     */
    private void parseMovieNFO(String nfo, Movie movie) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLEventReader r = factory.createXMLEventReader(new StringReader(nfo));

            boolean isMovieTag = false;
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                
                if (e.isStartElement()) {
                    String tag = e.toString();
                    if (tag.equalsIgnoreCase("<movie>")) {
                        isMovieTag = true;
                    } 
                    
                    if (isMovieTag) {
                        if (tag.equalsIgnoreCase("<title>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setTitle(val);
                            }
                        } else if (tag.equalsIgnoreCase("<originaltitle>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<rating>")) {
                            float val = XMLHelper.parseFloat(r);
                            if (val != 0.0f) {
                                movie.setRating(Math.round(val * 10f));
                            }
                        } else if (tag.equalsIgnoreCase("<year>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setYear(val);
                            }
                        } else if (tag.equalsIgnoreCase("<top250>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<votes>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<outline>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<plot>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setPlot(val);
                            }
                        } else if (tag.equalsIgnoreCase("<tagline>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<runtime>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setRuntime(val);
                            }
                        } else if (tag.equalsIgnoreCase("<thumb>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("<mpaa>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setCertification(val);
                            }
                        } else if (tag.equalsIgnoreCase("<playcount>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<watched>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<id>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, val);
                            }
                        } else if (tag.equalsIgnoreCase("<filenameandpath>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<trailer>")) {
                            String trailer = XMLHelper.getCData(r).trim();
                            if (!trailer.isEmpty()) {
                                TrailerFile tf = new TrailerFile();
                                tf.setNewFile(false);
                                tf.setFilename(trailer);
                                movie.addTrailerFile(tf);
                            }
                        } else if (tag.equalsIgnoreCase("<genre>")) {
                            Collection<String> genres = movie.getGenres();
                            List<String> newGenres = XMLHelper.parseList(XMLHelper.getCData(r), "|/,");
                            genres.addAll(newGenres);
                            movie.setGenres(genres);
                        } else if (tag.equalsIgnoreCase("<credits>")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("<director>")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setDirector(val);
                            }
                        } else if (tag.equalsIgnoreCase("<actor>")) {
                            String event = r.nextEvent().toString();
                            while (!event.equalsIgnoreCase("</actor>")) {
                                if (event.equalsIgnoreCase("<name>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (!val.isEmpty()) {
                                        movie.addActor(val);
                                    }
                                } else if (event.equalsIgnoreCase("<role>")) {
                                    // ignored
                                }
                                if (r.hasNext()) {
                                    event = r.nextEvent().toString();
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                } else if (e.isEndElement()) {
                    if (e.toString().equalsIgnoreCase("</movie>")) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed parsing NFO file for movie: " + movie.getTitle() + ". Please fix or remove it.");
        }
    }  
    
    
}
