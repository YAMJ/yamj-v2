package com.moviejukebox.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.TrailerFile;
import com.moviejukebox.plugin.DatabasePluginController;
import com.moviejukebox.plugin.ImdbPlugin;
import com.moviejukebox.plugin.TheTvDBPlugin;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.XMLHelper;

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
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static String fanartToken = ".fanart";
    private static String forceNFOEncoding = null; 
    private static String NFOdirectory = "";
    private static boolean parentDirs = false;

    /**
     * Search the IMDBb id of the specified movie in the NFO file if it exists.
     *
     * @param movie
     * @param movieDB
     */
    public static void scan(Movie movie, List<File> nfoFiles) {
        for (File nfoFile : nfoFiles) {
            logger.finest("Scanning NFO file for Infos : " + nfoFile.getName());
            // Set the NFO as dirty so that the information will be re-scanned at the appropriate points.
            movie.setDirtyNFO(true);

            String nfo = FileTools.readFileToString(nfoFile);

            if (!parseXMLNFO(nfo, movie, nfoFile)) {
                DatabasePluginController.scanNFO(nfo, movie);

                logger.finest("Scanning NFO for Poster URL");
                int urlStartIndex = 0;
                while (urlStartIndex >= 0 && urlStartIndex < nfo.length()) {
                    int currentUrlStartIndex = nfo.indexOf("http://", urlStartIndex);
                    if (currentUrlStartIndex >= 0) {
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
                            movie.setDirtyPoster(true);
                        } else {
                            urlStartIndex = currentUrlStartIndex + 3;
                        }
                    } else {
                        urlStartIndex = -1;
                    }
                }
            }
        }
    }
    
    public static List<File> locateNFOs(Movie movie) {
        List<File> nfos = new ArrayList<File>();
        
        String fn = movie.getContainerFile().getAbsolutePath();
        String localMovieDir = fn.substring(0, fn.lastIndexOf(File.separator)); // the full directory that the video file is in
        String localDirectoryName = localMovieDir.substring(localMovieDir.lastIndexOf(File.separator) + 1); // just the sub-directory the video file is in

        // If "fn" is a file then strip the extension from the file.
        if (movie.getContainerFile().isFile()) {
            fn = fn.substring(0, fn.lastIndexOf("."));
        } else {
            // *** First step is to check for VIDEO_TS
            // The movie is a directory, which indicates that this is a VIDEO_TS file
            // So, we should search for the file moviename.nfo in the sub-directory
            checkNFO(nfos, fn + fn.substring(fn.lastIndexOf(File.separator)));
        }

        // *** Second step is to check for the filename.nfo file
        // This file should be named exactly the same as the video file with an extension of "nfo" or "NFO"
        // E.G. C:\Movies\Bladerunner.720p.avi => Bladerunner.720p.nfo
        checkNFO(nfos, fn);
        
        if (!NFOdirectory.equals("")) {
            // *** Next step if we still haven't found the nfo file is to
            // search the NFO directory as specified in the moviejukebox.properties file
            String sLibraryPath = movie.getLibraryPath();
            if ((sLibraryPath.lastIndexOf("\\") == sLibraryPath.length()) || (sLibraryPath.lastIndexOf("/") == sLibraryPath.length())) {
                checkNFO(nfos, movie.getLibraryPath() + NFOdirectory + File.separator + movie.getBaseName());
            } else {
                checkNFO(nfos, movie.getLibraryPath() + File.separator + NFOdirectory + File.separator + movie.getBaseName());
            }
        }
        
        // *** Last step is to check for a directory wide NFO file.
        // This file should be named the same as the directory that it is in
        // E.G. C:\TV\Chuck\Season 1\Season 1.nfo
        // We search up through all containing directories up to the library root
        do {
            checkNFO(nfos, localMovieDir + File.separator + localDirectoryName);
            
            localMovieDir = fn.substring(0, localMovieDir.lastIndexOf(File.separator)); // parent directory
            localDirectoryName = localMovieDir.substring(localMovieDir.lastIndexOf(File.separator) + 1); // parent directory name
        } while (parentDirs && localMovieDir.length() >= movie.getLibraryPath().length() - 1); // up to the library root
        
        // we added the most specific ones first, and we want to parse those the last,
        // so nfo files in subdirectories can override values in directories above.
        Collections.reverse(nfos);
        
        return nfos;
    }

    /**
     * Check to see if the passed filename exists with nfo extensions
     *
     * @param checkNFOfilename
     *            (NO EXTENSION)
     * @return blank string if not found, filename if found
     */
    private static void checkNFO(List<File> nfoFiles, String checkNFOfilename) {
        // logger.finest("checkNFO = " + checkNFOfilename);
        File nfoFile = new File(checkNFOfilename + ".nfo");
        if (nfoFile.exists()) {
            nfoFiles.add(nfoFile);
        } else {
            nfoFile = new File(checkNFOfilename + ".NFO");
            if (nfoFile.exists()) {
                nfoFiles.add(nfoFile);
            }
        }
    }

    private static boolean parseXMLNFO(String nfo, Movie movie, File nfoFile) {
        boolean retval = true;
        if (nfo.indexOf("<movie") > -1 && parseMovieNFO(nfoFile, movie, nfo)) {
            return true;
        } else if (nfo.indexOf("<tvshow") > -1 && parseTVNFO(nfoFile, movie, nfo)) {
            return true;
        } else {
            retval = false;
        }
        return retval;
    }

    /**
     * Create an XML reader for file. Use forced encoding if specified.
     * @param nfoFile File to read.
     * @param nfo Content of the XML file. Used only for the encoding detection. 
     * @return New XML reader.
     * @throws FactoryConfigurationError
     * @throws XMLStreamException
     * @throws FileNotFoundException
     */
    private static XMLEventReader createXMLReader(File nfoFile, String nfo)
            throws FactoryConfigurationError, XMLStreamException, FileNotFoundException {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        // TODO Make the encoding detection more explicit
        boolean fileContainsEncoding = false;
        if (nfo != null) {
            int i = nfo.indexOf("encoding");
            fileContainsEncoding = (i > 0 && i < 100);
        }

        XMLEventReader r = (forceNFOEncoding != null && !fileContainsEncoding) ? factory.createXMLEventReader(new FileInputStream(nfoFile), forceNFOEncoding)
                        : factory.createXMLEventReader(new FileInputStream(nfoFile));
        return r;
    }

    /**
     * Used to parse out the XBMC nfo xml data for movies
     *
     * @param xmlFile
     * @param movie
     */
    private static boolean parseMovieNFO(File nfoFile, Movie movie, String nfo) {
        try {
            XMLEventReader r = createXMLReader(nfoFile, nfo);

            boolean isMovieTag = false;
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();
                
                if (e.isStartElement()) {
                    String tag = e.asStartElement().getName().toString();
                    logger.finest("In parseMovieNFO found new startElement=" + tag);
                    if (tag.equalsIgnoreCase("movie")) {
                        isMovieTag = true;
                    }

                    if (isMovieTag) {
                        if (tag.equalsIgnoreCase("title")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setTitle(val);
                                movie.setOverrideTitle(true);
                            }
                        } else if (tag.equalsIgnoreCase("originaltitle")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("rating")) {
                            float val = XMLHelper.parseFloat(r);
                            if (val != 0.0f) {
                                movie.setRating(Math.round(val * 10f));
                            }
                        } else if (tag.equalsIgnoreCase("year")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setYear(val);
                            }
                        } else if (tag.equalsIgnoreCase("top250")) {
                            int val = XMLHelper.parseInt(r);
                            if (val > 0) {
                                movie.setTop250(val);
                            }
                        } else if (tag.equalsIgnoreCase("votes")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("outline")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setOutline(val);
                            }
                        } else if (tag.equalsIgnoreCase("plot")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setPlot(val);
                            }
                        } else if (tag.equalsIgnoreCase("tagline")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("runtime")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setRuntime(val);
                            }
                        } else if (tag.equalsIgnoreCase("thumb")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("fanart")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setFanartURL(val);
                                movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                            }
                        } else if (tag.equalsIgnoreCase("mpaa")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                // Issue 333
                                if (val.startsWith("Rated ")) {
                                    int start = 6; // "Rated ".length()
                                    int pos = val.indexOf(" on appeal for ", start);
                                    if (pos == -1) {
                                        pos = val.indexOf(" for ", start);
                                    }
                                    if (pos > start) {
                                        val = val.substring(start, pos);
                                    } else {
                                        val = val.substring(start);
                                    }
                                }
                                movie.setCertification(val);
                            }
                        } else if (tag.equalsIgnoreCase("playcount")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("watched")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("id")) {
                            Attribute movieDbIdAttribute = e.asStartElement().getAttributeByName(new QName("moviedb"));
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                if (movieDbIdAttribute != null) { // if we have a moviedb attribute
                                    movie.setId(movieDbIdAttribute.getValue(), val); // we store the Id for this movieDb
                                    logger.finest("In parseMovieNFO Id=" + val + " found for movieDB=" + movieDbIdAttribute.getValue());
                                } else {
                                    movie.setId(ImdbPlugin.IMDB_PLUGIN_ID, val); // without attribute we assume it's an IMDB Id
                                    logger.finest("In parseMovieNFO Id=" + val + " found for default IMDB");
                                }
                            }
                        } else if (tag.equalsIgnoreCase("filenameandpath")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("trailer")) {
                            String trailer = XMLHelper.getCData(r).trim();
                            if (!trailer.isEmpty()) {
                                TrailerFile tf = new TrailerFile();
                                tf.setNewFile(false);
                                tf.setFilename(trailer);
                                movie.addTrailerFile(tf);
                            }
                        } else if (tag.equalsIgnoreCase("genre")) {
                            Collection<String> genres = movie.getGenres();
                            List<String> newGenres = XMLHelper.parseList(XMLHelper.getCData(r), "|/,");
                            genres.addAll(newGenres);
                            movie.setGenres(genres);
                        } else if (tag.equalsIgnoreCase("set")) {
                            String set = XMLHelper.getCData(r);
                            Attribute orderAttribute = e.asStartElement().getAttributeByName(new QName("order"));
                            movie.addSet(set, orderAttribute == null ? null : Integer.parseInt(orderAttribute.getValue()));
                        } else if (tag.equalsIgnoreCase("credits")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("director")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setDirector(val);
                            }
                        } else if (tag.equalsIgnoreCase("actor")) {
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
                    // logger.finest("In parseMovieNFO found new endElement=" + e.asEndElement().getName().toString());
                    if (e.asEndElement().getName().toString().equalsIgnoreCase("movie")) {
                        break;
                    }
                }
            }
            
            return isMovieTag;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed parsing NFO file for movie: " + movie.getTitle() + ". Please fix or remove it.");
        }
        
        return false;
    }

    /**
     * Used to parse out the XBMC nfo xml data for tv series
     *
     * @param xmlFile
     * @param movie
     */
    private static boolean parseTVNFO(File nfoFile, Movie movie, String nfo) {
        try {
            XMLEventReader r = createXMLReader(nfoFile, nfo);

            boolean isTVTag = false;
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartElement()) {
                    String tag = e.asStartElement().getName().toString();
                    if (tag.equalsIgnoreCase("tvshow")) {
                        isTVTag = true;
                    }

                    if (isTVTag) {
                        if (tag.equalsIgnoreCase("title")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setTitle(val);
                                movie.setOverrideTitle(true);
                            }
                        } else if (tag.equalsIgnoreCase("id")) {
                            Attribute movieDbIdAttribute = e.asStartElement().getAttributeByName(new QName("moviedb"));
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                if (movieDbIdAttribute != null) { // if we have a moviedb attribute
                                    movie.setId(movieDbIdAttribute.getValue(), val); // we store the Id for this movieDb
                                    logger.finest("In parseTVNFO Id=" + val + " found for movieDB=" + movieDbIdAttribute.getValue());
                                } else {
                                    movie.setId(TheTvDBPlugin.THETVDB_PLUGIN_ID, val); // without attribute we assume it's a TheTVDB Id
                                    logger.finest("In parseTVNFO Id=" + val + " found for default TheTVDB");
                                }
                            }
                        } else if (tag.equalsIgnoreCase("rating")) {
                            float val = XMLHelper.parseFloat(r);
                            if (val != 0.0f) {
                                movie.setRating(Math.round(val * 10f));
                            }
                        } else if (tag.equalsIgnoreCase("mpaa")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                // Issue 333
                                if (val.startsWith("Rated ")) {
                                    int start = 6; // "Rated ".length()
                                    int pos = val.indexOf(" on appeal for ", start);
                                    if (pos == -1) {
                                        pos = val.indexOf(" for ", start);
                                    }
                                    if (pos > start) {
                                        val = val.substring(start, pos);
                                    } else {
                                        val = val.substring(start);
                                    }
                                }
                                movie.setCertification(val);
                            }
                        } else if (tag.equalsIgnoreCase("season")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("episode")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("votes")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("displayseason")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("displayepisode")) {
                            // ignored
                        } else if (tag.equalsIgnoreCase("plot")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setPlot(val);
                            }
                        } else if (tag.equalsIgnoreCase("genre")) {
                            Collection<String> genres = movie.getGenres();
                            List<String> newGenres = XMLHelper.parseList(XMLHelper.getCData(r), "|/,");
                            genres.addAll(newGenres);
                            movie.setGenres(genres);
                        } else if (tag.equalsIgnoreCase("premiered")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                try {
                                    movie.setReleaseDate(val);
                                    Date date = dateFormat.parse(val);
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(date);
                                    movie.setYear("" + cal.get(Calendar.YEAR));
                                } catch (Exception ignore) {
                                }
                            }
                        } else if (tag.equalsIgnoreCase("studio")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setCompany(val);
                            }
                        } else if (tag.equalsIgnoreCase("thumb")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setPosterURL(val);
                            }
                        } else if (tag.equalsIgnoreCase("fanart")) {
                            String val = XMLHelper.getCData(r);
                            if (!val.isEmpty()) {
                                movie.setFanartURL(val);
                                movie.setFanartFilename(movie.getBaseName() + fanartToken + ".jpg");
                            }
                        } else if (tag.equalsIgnoreCase("trailer")) {
                            String trailer = XMLHelper.getCData(r).trim();
                            if (!trailer.isEmpty()) {
                                TrailerFile tf = new TrailerFile();
                                tf.setNewFile(false);
                                tf.setFilename(trailer);
                                movie.addTrailerFile(tf);
                            }
                        } else if (tag.equalsIgnoreCase("actor")) {
                            String event = r.nextEvent().toString();
                            while (!event.equalsIgnoreCase("</actor>")) {
                                if (event.equalsIgnoreCase("<name>")) {
                                    String val = XMLHelper.getCData(r);
                                    if (!val.isEmpty()) {
                                        movie.addActor(val);
                                    }
                                } else if (event.equalsIgnoreCase("<role>")) {
                                    // ignored
                                } else if (event.equalsIgnoreCase("<thumb>")) {
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
                    if (e.asEndElement().getName().toString().equalsIgnoreCase("tvshow")) {
                        isTVTag = false;
                    }
                }
            }
            return isTVTag;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed parsing NFO file for tvshow: " + movie.getTitle() + ". Please fix or remove it.");
        }
        
        return false;
    }

    public static String getForceNFOEncoding() {
        return forceNFOEncoding;
    }

    public static void setForceNFOEncoding(String forceNFOEncoding) {
        MovieNFOScanner.forceNFOEncoding = forceNFOEncoding;
    }

    public static String getFanartToken() {
        return fanartToken;
    }

    public static void setFanartToken(String fanartToken) {
        MovieNFOScanner.fanartToken = fanartToken;
    }

    public static String getNFOdirectory() {
        return NFOdirectory;
    }

    public static void setNFOdirectory(String odirectory) {
        NFOdirectory = odirectory;
    }
    
    public static void setParentDirs(boolean parentDirs) {
        MovieNFOScanner.parentDirs = parentDirs;
    }
}
