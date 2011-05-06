/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
package com.moviejukebox.writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.IndexInfo;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.XMLWriter;

/**
 * Generate HTML pages from XML movies and indexes
 * 
 * @author Julien
 * @author artem.gratchev
 */
public class MovieJukeboxHTMLWriter {

    private static Logger logger = Logger.getLogger("moviejukebox");
    private boolean forceHTMLOverwrite;
    private int nbMoviesPerPage;
    private int nbTvShowsPerPage;
    private static String skinHome;
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
    // private static String str_categoriesIndexList = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Rating,Year,Library,Set");
    // private static List<String> categoriesIndexList = Arrays.asList(str_categoriesIndexList.split(","));
    // private static int categoriesMinCount = PropertiesUtil.getIntProperty("mjb.categories.minCount", "3"));
    private static String playlistIgnoreExtensions = PropertiesUtil.getProperty("mjb.playlist.IgnoreExtensions", "iso,img");
    private static File playlistFile;
    private static String indexFile = "../" + PropertiesUtil.getProperty("mjb.indexFile", "index.htm");
    private static String myiHomeIP = PropertiesUtil.getProperty("mjb.myiHome.IP", "");
    private static boolean generateMultiPartPlaylist = PropertiesUtil.getBooleanProperty("mjb.playlist.generateMultiPart", "true");

    public MovieJukeboxHTMLWriter() {
        forceHTMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceHTMLOverwrite", "false");
        nbMoviesPerPage = PropertiesUtil.getIntProperty("mjb.nbThumbnailsPerPage", "10");
        nbTvShowsPerPage = PropertiesUtil.getIntProperty("mjb.nbTvThumbnailsPerPage", "0"); // If 0 then use the Movies setting
        if (nbTvShowsPerPage == 0) {
            nbTvShowsPerPage = nbMoviesPerPage;
        }
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
        playlistFile = new File("playlist.xsl");

        // Issue 310
        String transformerFactory = PropertiesUtil.getProperty("javax.xml.transform.TransformerFactory", null);
        if (transformerFactory != null) {
            System.setProperty("javax.xml.transform.TransformerFactory", transformerFactory);
        }
    }

    public void generateMovieDetailsHTML(Jukebox jukebox, Movie movie) {
        try {
            String baseName = movie.getBaseName();
            String tempFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + baseName;
            File tempXmlFile = new File(tempFilename + ".xml");
            File oldXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + ".xml");

            FileTools.addJukeboxFile(baseName + ".xml");
            String indexList = PropertiesUtil.getProperty("mjb.view.detailList", "detail.xsl");
            for (final String indexStr : indexList.split(",")) {
                String Suffix = "";
                if (!indexStr.equals("detail.xsl")) {
                    Suffix = indexStr.replace("detail", "").replace(".xsl", "");
                }

                File finalHtmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + Suffix + ".html");
                File tempHtmlFile = new File(tempFilename + Suffix + ".html");
                Source xmlSource;

                FileTools.addJukeboxFile(baseName + Suffix + ".html");

                if (!finalHtmlFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
                    // Issue 216: If the HTML is deleted the generation fails because it looks in the temp directory and not
                    // the original source directory
                    if (tempXmlFile.exists()) {
                        // Use the temp file
                        xmlSource = new StreamSource(tempXmlFile);
                    } else {
                        // Use the file in the original directory
                        xmlSource = new StreamSource(oldXmlFile);
                    }
                    Result xmlResult = new StreamResult(tempHtmlFile);

                    if (xmlSource != null && xmlResult != null) {
                        File skinFile = new File(skinHome, indexStr);
                        Transformer transformer = getTransformer(skinFile, jukebox.getJukeboxRootLocationDetails());
                        transformer.transform(xmlSource, xmlResult);
                    } else {
                        logger.error("HTMLWriter: Unable to transform XML for video " + movie.getBaseFilename() + " source: " + (xmlSource == null ? true : false)
                                        + " result: " + (xmlResult == null ? true : false));
                    }
                }
            }
        } catch (Exception error) {
            logger.error("HTMLWriter: Failed generating HTML for movie " + movie.getBaseFilename());
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    /**
     * Generates a playlist per part of the video. Used primarily with TV Series
     * 
     * @param rootPath
     * @param tempRootPath
     * @param movie
     * @return List of generated file names
     */
    public Collection<String> generatePlaylist(Jukebox jukebox, Movie movie) {
        Collection<String> fileNames = new ArrayList<String>();

        if (playlistFile == null || playlistFile.equals("")) {
            return fileNames;
        }

        MovieFile[] movieFileArray = movie.getFiles().toArray(new MovieFile[movie.getFiles().size()]);

        try {
            String baseName = movie.getBaseName();
            String tempFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + baseName;
            File tempXmlFile = new File(tempFilename + ".xml");
            File oldXmlFile = new File(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + ".xml");
            final String filenameSuffix = ".playlist.jsp";
            File finalPlaylistFile = new File(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + filenameSuffix);
            File tempPlaylistFile = new File(tempFilename + filenameSuffix);
            Source xmlSource;

            fileNames.add(baseName + filenameSuffix);

            // Issue 884: Remove ISO and IMG files from playlists
            int partCount = 0;
            for (int i = 0; i < movieFileArray.length; i++) {
                MovieFile moviePart = movieFileArray[i];
                String partExt = new String(moviePart.getFilename().substring(moviePart.getFilename().lastIndexOf(".") + 1));
                if (playlistIgnoreExtensions.indexOf(partExt) > -1) {
                    partCount++;
                }
            }
            if (partCount > 0) {
                // Note this will skip playlist generation for any movie that has an "mjb.playlist.ignoreextensions" entry.
                logger.debug("HTMLWriter: Playlist for " + movie.getTitle() + " skipped - All parts are in mjb.playlist.IgnoreExtensions");
                return fileNames;
            } // Issue 884

            if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
                tempPlaylistFile.getParentFile().mkdirs();

                Transformer transformer = getTransformer(playlistFile, jukebox.getJukeboxRootLocationDetails());

                if (tempXmlFile.exists()) {
                    // Use the temp file
                    xmlSource = new StreamSource(tempXmlFile);
                } else {
                    // Use the file in the original directory
                    xmlSource = new StreamSource(oldXmlFile);
                }
                Result xmlResult = new StreamResult(tempPlaylistFile);

                transformer.transform(xmlSource, xmlResult);

                removeBlankLines(tempPlaylistFile.getAbsolutePath());

                fileNames.add(baseName + filenameSuffix);
            }
        } catch (Exception error) {
            logger.error("HTMLWriter: Failed generating playlist for movie " + movie);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }

        // if the multi part playlists are not required
        if (generateMultiPartPlaylist) {
            try {
                if (movie.getFiles().size() > 1) {
                    for (int i = 0; i < movieFileArray.length; i++) {
                        fileNames.add(generateSimplePlaylist(jukebox, movie, movieFileArray, i));
                    }
                }
            } catch (Exception error) {
                logger.error("HTMLWriter: Failed generating playlist for movie " + movie);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.error(eResult.toString());
            }
        }

        return fileNames;
    }

    /**
     * Remove blank lines from the file The PCH does not like blank lines in the playlist.jsp files, so this routine will remove them This routine is only
     * called for the base playlist as this is transformed with the playlist.xsl file and therefore could end up with blank lines in it
     * 
     * @param filename
     */
    private void removeBlankLines(String filename) {
        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();
        FileWriter outFile = null;

        try {
            reader = new BufferedReader(new FileReader(filename));
            String br = "";

            while ((br = reader.readLine()) != null) {
                if (br.trim().length() > 0) {
                    sb.append(br + "\n");
                }
            }
            reader.close();

            outFile = new FileWriter(filename);
            outFile.write(sb.toString());
            outFile.flush();

        } catch (Exception error) {
            logger.debug("HTMLWriter: Failed deleting blank lines from " + filename);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        } finally {
            try {
                if (outFile != null) {
                    outFile.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return;
    }

    /**
     * Generate playlist with old simple method. The playlist is to be used for playing episodes starting from each episode separately.
     * 
     * @param rootPath
     * @param tempRootPath
     * @param movie
     * @param movieFiles
     * @param offset
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @return generated file name
     */
    private String generateSimplePlaylist(Jukebox jukebox, Movie movie, MovieFile[] movieFiles, int offset) throws FileNotFoundException,
                    UnsupportedEncodingException {
        String fileSuffix = ".playlist" + movieFiles[offset % movieFiles.length].getFirstPart() + ".jsp";
        String baseName = movie.getBaseName();
        String tempFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + baseName;
        File finalPlaylistFile = new File(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + fileSuffix);
        File tempPlaylistFile = new File(tempFilename + fileSuffix);

        if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
            tempPlaylistFile.getParentFile().mkdirs();

            PrintWriter writer = new PrintWriter(tempPlaylistFile, "UTF-8");

            // Issue 237 - Add in the IP address of the MyiHome server so the playlist will work.
            // Issue 237 - It is perfectly valid for "mjb.myiHome.IP" to be blank, in fact this is
            // the normal method for stand alone YAMJ
            for (int i = 0; i < movieFiles.length; i++) {
                MovieFile part = movieFiles[(i + offset) % movieFiles.length];
                // write one line each in the format "name|0|0|IP/path" replacing an | that may exist in the title
                writer.println(movie.getTitle().replace('|', ' ') + " " + part.getFirstPart() + "|0|0|" + myiHomeIP + part.getFilename() + "|");
            }
            writer.flush();
            writer.close();
        }
        return baseName + fileSuffix;
    }

    /**
     * Generate the mjb.indexFile page from a template. If the template is not found, then create a default index page
     * 
     * @param jukebox
     * @param library
     * @param indexFilename
     */
    public void generateMainIndexHTML(Jukebox jukebox, Library library) {
        File jukeboxIndexFile = new File(skinHome, "jukebox-index.xsl");

        if (jukeboxIndexFile.exists()) {
            generateTransformedIndexHTML(jukebox, library);
        } else {
            generateDefaultIndexHTML(jukebox, library);
        }
    }

    /**
     * Use an xsl file to generate the jukebox index file
     * 
     * @param jukebox
     * @param library
     */
    private void generateTransformedIndexHTML(Jukebox jukebox, Library library) {
        logger.debug("Generating Index file from jukebox-index.xsl");

        XMLWriter writer = null;

        String homePage = PropertiesUtil.getProperty("mjb.homePage", "");
        if (homePage.length() == 0) {
            String defCat = library.getDefaultCategory();
            if (defCat != null) {
                homePage = FileTools.createPrefix("Other", HTMLTools.encodeUrl(FileTools.makeSafeFilename(defCat))) + "1";
            } else {
                // figure out something better to do here
                logger.info("HTMLWriter: No categories were found, so you should specify mjb.homePage in the config file.");
            }
        }

        try {
            // Create the index.xml file with some properties in it.
            File indexFile = new File(jukebox.getJukeboxTempLocation(), "index.xml");
            indexFile.getParentFile().mkdirs();
            writer = new XMLWriter(indexFile);

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("index");

            writer.writeStartElement("detailsDirName");
            writer.writeCharacters(jukebox.getDetailsDirName());
            writer.writeEndElement();

            writer.writeStartElement("homePage");
            writer.writeCharacters(homePage);
            writer.writeEndElement();

            writer.writeEndElement(); // index
            writer.writeEndDocument();
            writer.close();

            // Now generate the HTML from the XLST
            File htmlFile = new File(jukebox.getJukeboxTempLocation(), PropertiesUtil.getProperty("mjb.indexFile", "index.htm"));
            FileTools.addJukeboxFile(indexFile.getName());
            FileTools.addJukeboxFile(htmlFile.getName());

            Transformer transformer = getTransformer(new File(skinHome, "jukebox-index.xsl"), jukebox.getJukeboxTempLocation());

            Source xmlSource = new StreamSource(indexFile);
            Result xmlResult = new StreamResult(htmlFile);

            transformer.transform(xmlSource, xmlResult);

        } catch (Exception error) {
            logger.error("HTMLWriter: Failed generating jukebox index.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    /**
     * Generate a simple jukebox index file from scratch
     * 
     * @param jukebox
     * @param library
     */
    private void generateDefaultIndexHTML(Jukebox jukebox, Library library) {
        try {
            File htmlFile = new File(jukebox.getJukeboxTempLocation(), PropertiesUtil.getProperty("mjb.indexFile", "index.htm"));
            htmlFile.getParentFile().mkdirs();

            OutputStream fos = FileTools.createFileOutputStream(htmlFile);
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(fos, "UTF-8");

            String homePage = PropertiesUtil.getProperty("mjb.homePage", "");
            if (homePage.length() == 0) {
                String defCat = library.getDefaultCategory();
                if (defCat != null) {
                    homePage = FileTools.createPrefix("Other", HTMLTools.encodeUrl(FileTools.makeSafeFilename(defCat))) + "1";
                } else {
                    // figure out something better to do here
                    logger.info("HTMLWriter: No categories were found, so you should specify mjb.homePage in the config file.");
                }
            }

            writer.writeStartDocument();
            writer.writeStartElement("html");
            writer.writeStartElement("head");

            writer.writeStartElement("meta");
            writer.writeAttribute("name", "YAMJ");
            writer.writeAttribute("content", "MovieJukebox");
            writer.writeEndElement();

            writer.writeStartElement("meta");
            writer.writeAttribute("HTTP-EQUIV", "Content-Type");
            writer.writeAttribute("content", "text/html; charset=UTF-8");
            writer.writeEndElement();

            writer.writeStartElement("meta");
            writer.writeAttribute("HTTP-EQUIV", "REFRESH");
            writer.writeAttribute("content", "0; url=" + jukebox.getDetailsDirName() + '/' + homePage + ".html");
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndElement();
            writer.close();
            fos.close();
        } catch (Exception error) {
            logger.error("HTMLWriter: Failed generating HTML library index.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    public void generateMoviesCategoryHTML(Jukebox jukebox, Library library, String filename, String template, boolean isDirty) {
        try {
            // Issue 1886: Html indexes recreated every time
            String destFolder = jukebox.getJukeboxRootLocationDetails();
            File oldFile = FileTools.fileCache.getFile(destFolder + File.separator + filename + ".html");
            if (oldFile.exists() && !isDirty) {
                return;
            }

            Source xmlSource;
            File detailsFolder = jukebox.getJukeboxTempLocationDetailsFile();
            File xmlFile = new File(detailsFolder, filename + ".xml");
            File oldXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + filename + ".xml");
            File htmlFile = new File(detailsFolder, filename + ".html");

            htmlFile.getParentFile().mkdirs();

            if (xmlFile.exists()) {
                FileTools.addJukeboxFile(xmlFile.getName());
                xmlSource = new StreamSource(xmlFile);
            } else {
                xmlSource =  new StreamSource(oldXmlFile);
            }

            FileTools.addJukeboxFile(htmlFile.getName());
            Result xmlResult = new StreamResult(htmlFile);

            Transformer transformer = getTransformer(new File(skinHome, template), jukebox.getJukeboxTempLocation());
            transformer.transform(xmlSource, xmlResult);
        } catch (Exception error) {
            logger.error("HTMLWriter: Failed generating HTML library category index.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    public void generateMoviesIndexHTML(final Jukebox jukebox, final Library library, ThreadExecutor<Void> tasks) throws Throwable {
        tasks.restart();
        for (final IndexInfo idx : library.getGeneratedIndexes()) {
            if (idx.canSkip) { // this is evaluated during XML indexing
                logger.debug("HTMLWriter: Category " + idx.categoryName + " " + idx.key + " no change detected, skipping HTML generation.");

                // Add the index files to the cache so they aren't deleted
                for (int page = 1; page <= idx.pages; page++) {
                    FileTools.addJukeboxFile(idx.baseName + page + ".xml");
                    FileTools.addJukeboxFile(idx.baseName + page + ".html");
                }
            } else {
                tasks.submit(new Callable<Void>() {
                    public Void call() {
                        for (int page = 1; page <= idx.pages; page++) {
                            writeSingleIndexPage(jukebox, idx, page);
                        }
                        return null;
                    }
                });
            }
        }

        tasks.waitFor();

    }

    private void writeSingleIndexPage(Jukebox jukebox, IndexInfo idx, int page) throws TransformerFactoryConfigurationError {
        try {
            File detailsDir = jukebox.getJukeboxTempLocationDetailsFile();
            detailsDir.mkdirs();

            String filename = idx.baseName + page;

            File xmlFile = new File(detailsDir, filename + ".xml");

            FileTools.addJukeboxFile(xmlFile.getName());
            String indexList = PropertiesUtil.getProperty("mjb.view.indexList", "index.xsl");
            for (final String indexStr : indexList.split(",")) {
                String Suffix = "";
                if (!indexStr.equals("index.xsl")) {
                    Suffix = indexStr.replace("index", "").replace(".xsl", "");
                }

                File htmlFile = new File(detailsDir, filename + Suffix + ".html");
                FileTools.addJukeboxFile(htmlFile.getName());

                File transformCatKey = new File(skinHome, FileTools.makeSafeFilename(idx.categoryName + "_" + idx.key) + ".xsl");
                File transformCategory = new File(skinHome, FileTools.makeSafeFilename(idx.categoryName) + ".xsl");
                File transformBase = new File(skinHome, indexStr);

                Transformer transformer;

                if (transformCatKey.exists()) {
                    logger.debug("HTMLWriter: Using CategoryKey transformation " + transformCatKey.getName() + " for " + xmlFile.getName());
                    transformer = getTransformer(transformCatKey, jukebox.getJukeboxTempLocationDetails());
                } else if (transformCategory.exists()) {
                    logger.debug("HTMLWriter: Using Category transformation " + transformCategory.getName() + " for " + xmlFile.getName());
                    transformer = getTransformer(transformCategory, jukebox.getJukeboxTempLocationDetails());
                } else {
                    transformer = getTransformer(transformBase, jukebox.getJukeboxTempLocationDetails());
                }

                // Transformer transformer = getTransformer(new File(skinHome, "index.xsl"), rootPath);
                Source xmlSource = new StreamSource(xmlFile);
                Result xmlResult = new StreamResult(htmlFile);

                transformer.transform(xmlSource, xmlResult);
            }
        } catch (Exception error) {
            logger.error("HTMLWriter: Failed generating HTML library index for Category: " + idx.categoryName + ", Key: " + idx.key + ", Page: " + page);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.error(eResult.toString());
        }
    }

    /**
     * Creates and caches Transformer, one for every thread/xsl file.
     */
    public static Transformer getTransformer(File xslFile, String styleSheetTargetRootPath) throws TransformerConfigurationException {
        /*
         * Removed caching of transformer, as saxon keeps all parsed documents in memory, causing memory leaks.
         * Creating a new transformer every time doesn't consume too much time and has no impact on performance.
         * It lets YAMJ save lot of memory.
         * @author Vincent
         */
        Source xslSource = new StreamSource(xslFile);
        
        // Sometimes the StreamSource doesn't return an object and we get a null pointer exception, so check it and try loading it again
        if (xslSource == null || xslSource.equals(null)) {
            xslSource = new StreamSource(xslFile);
        }
        
        Transformer transformer = transformerFactory.newTransformer(xslSource);
        transformer.setParameter("homePage", indexFile);
        transformer.setParameter("rootPath", new File(styleSheetTargetRootPath).getAbsolutePath().replace('\\', '/'));
        for (Entry<Object, Object> e : PropertiesUtil.getEntrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                transformer.setParameter(e.getKey().toString(), e.getValue().toString());
            }
        }
        return transformer;
    }
}
