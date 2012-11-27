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
package com.moviejukebox.writer;

import com.moviejukebox.model.*;
import com.moviejukebox.tools.*;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;

/**
 * Generate HTML pages from XML movies and indexes
 *
 * @author Julien
 * @author artem.gratchev
 */
public class MovieJukeboxHTMLWriter {

    private static final Logger logger = Logger.getLogger(MovieJukeboxHTMLWriter.class);
    private static final String LOG_MESSAGE = "HTMLWriter: ";
    private static final String EXT_XML = ".xml";
    private static final String EXT_HTML = ".html";
    private static final String EXT_XSL = ".xsl";
    private boolean forceHTMLOverwrite = PropertiesUtil.getBooleanProperty("mjb.forceHTMLOverwrite", FALSE);
    private String peopleFolder;
    private static String skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
    private static TransformerFactory transformerFactory = TransformerFactory.newInstance();
    private static String playlistIgnoreExtensions = PropertiesUtil.getProperty("mjb.playlist.IgnoreExtensions", "iso,img");
    private static File playlistFile = new File("playlist.xsl");
    private static String indexHtmFile = "../" + PropertiesUtil.getProperty("mjb.indexFile", "index.htm");
    private static String myiHomeIP = PropertiesUtil.getProperty("mjb.myiHome.IP", "");
    private static boolean generateMultiPartPlaylist = PropertiesUtil.getBooleanProperty("mjb.playlist.generateMultiPart", TRUE);
    private static int maxRetryCount = 3;   // The number of times to retry writing a HTML page

    public MovieJukeboxHTMLWriter() {

        // Issue 1947: Cast enhancement - option to save all related files to a specific folder
        peopleFolder = PropertiesUtil.getProperty("mjb.people.folder", "");
        if (StringTools.isNotValidString(peopleFolder)) {
            peopleFolder = "";
        } else if (!peopleFolder.endsWith(File.separator)) {
            peopleFolder += File.separator;
        }

        // Issue 310
        String transformerFactoryName = PropertiesUtil.getProperty("javax.xml.transform.TransformerFactory", null);
        if (transformerFactoryName != null) {
            System.setProperty("javax.xml.transform.TransformerFactory", transformerFactoryName);
        }
    }

    public void generateMovieDetailsHTML(Jukebox jukebox, Movie movie) {
        try {
            String baseName = movie.getBaseName();
            String tempFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + baseName;
            File tempXmlFile = new File(tempFilename + EXT_XML);
            File oldXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + EXT_XML);

            FileTools.addJukeboxFile(baseName + EXT_XML);
            String indexList = PropertiesUtil.getProperty("mjb.view.detailList", "detail.xsl");
            for (final String indexStr : indexList.split(",")) {
                String suffix = "";
                if (!indexStr.equals("detail.xsl")) {
                    suffix = indexStr.replace("detail", "").replace(EXT_XSL, "");
                }

                File finalHtmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + suffix + EXT_HTML);
                File tempHtmlFile = new File(tempFilename + suffix + EXT_HTML);
                Source xmlSource;

                FileTools.addJukeboxFile(baseName + suffix + EXT_HTML);

                if (!finalHtmlFile.exists() || forceHTMLOverwrite || movie.isDirty(DirtyFlag.INFO) || movie.isDirty(DirtyFlag.WATCHED)) {
                    
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

                        doTransform(transformer, xmlSource, xmlResult, "Movie: " + movie.getBaseFilename());
                    } else {
                        logger.error(LOG_MESSAGE + "Unable to transform XML for video " + movie.getBaseFilename() + " source: " + (xmlSource == null ? true : false)
                                + " result: " + (xmlResult == null ? true : false));
                    }
                }
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed generating HTML for movie " + movie.getBaseFilename());
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    public void generatePersonDetailsHTML(Jukebox jukebox, Person person) {
        try {
            String baseName = person.getFilename();
            String tempFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + peopleFolder + baseName;
            File tempXmlFile = new File(tempFilename + EXT_XML);
            File oldXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + baseName + EXT_XML);
            tempXmlFile.getParentFile().mkdirs();

            FileTools.addJukeboxFile(baseName + EXT_XML);
            String indexList = PropertiesUtil.getProperty("mjb.view.personList", "people.xsl");
            for (final String indexStr : indexList.split(",")) {
                String suffix = "";
                if (!indexStr.equals("people.xsl")) {
                    suffix = indexStr.replace("people", "").replace(EXT_XSL, "");
                }

                File finalHtmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + peopleFolder + baseName + suffix + EXT_HTML);
                File tempHtmlFile = new File(tempFilename + suffix + EXT_HTML);
                Source xmlSource;

                FileTools.addJukeboxFile(baseName + suffix + EXT_HTML);

                if (!finalHtmlFile.exists() || forceHTMLOverwrite || person.isDirty()) {
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
                        doTransform(transformer, xmlSource, xmlResult, "Person: " + person.getName());
                    } else {
                        logger.error(LOG_MESSAGE + "Unable to transform XML for person " + person.getName() + " source: " + (xmlSource == null ? true : false)
                                + " result: " + (xmlResult == null ? true : false));
                    }
                }
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed generating HTML for person " + person.getName());
            logger.error(SystemTools.getStackTrace(error));
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

        if (playlistFile == null) {
            return fileNames;
        }

        MovieFile[] movieFileArray = movie.getFiles().toArray(new MovieFile[movie.getFiles().size()]);

        try {
            String baseName = movie.getBaseName();
            String tempFilename = jukebox.getJukeboxTempLocationDetails() + File.separator + baseName;
            File tempXmlFile = new File(tempFilename + EXT_XML);
            File oldXmlFile = new File(jukebox.getJukeboxRootLocationDetails() + File.separator + baseName + EXT_XML);
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
                logger.debug(LOG_MESSAGE + "Playlist for " + movie.getTitle() + " skipped - All parts are in mjb.playlist.IgnoreExtensions");
                return fileNames;
            } // Issue 884

            if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty(DirtyFlag.INFO)) {
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

                doTransform(transformer, xmlSource, xmlResult, "Movie: " + movie.getBaseName());

                removeBlankLines(tempPlaylistFile.getAbsolutePath());

                fileNames.add(baseName + filenameSuffix);
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed generating playlist for movie " + movie);
            logger.error(SystemTools.getStackTrace(error));
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
                logger.error(LOG_MESSAGE + "Failed generating playlist for movie " + movie);
                logger.error(SystemTools.getStackTrace(error));
            }
        }

        return fileNames;
    }

    /**
     * Remove blank lines from the file The PCH does not like blank lines in the playlist.jsp files, so this routine
     * will remove them This routine is only called for the base playlist as this is transformed with the playlist.xsl
     * file and therefore could end up with blank lines in it
     *
     * @param filename
     */
    private void removeBlankLines(String filename) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        FileWriter outFile = null;

        try {
            reader = new BufferedReader(new FileReader(filename));
            String br;

            while ((br = reader.readLine()) != null) {
                if (br.trim().length() > 0) {
                    sb.append(br).append("\n");
                }
            }
            reader.close();

            outFile = new FileWriter(filename);
            outFile.write(sb.toString());
            outFile.flush();

        } catch (Exception error) {
            logger.debug(LOG_MESSAGE + "Failed deleting blank lines from " + filename);
            logger.error(SystemTools.getStackTrace(error));
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
    }

    /**
     * Generate playlist with old simple method. The playlist is to be used for playing episodes starting from each
     * episode separately.
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

        if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty(DirtyFlag.INFO)) {
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

        String homePage = PropertiesUtil.getProperty("mjb.homePage", "");
        if (homePage.length() == 0) {
            String defCat = library.getDefaultCategory();
            if (defCat != null) {
                homePage = FileTools.createPrefix("Other", HTMLTools.encodeUrl(FileTools.makeSafeFilename(defCat))) + "1";
            } else {
                // figure out something better to do here
                logger.info(LOG_MESSAGE + "No categories were found, so you should specify mjb.homePage in the config file.");
            }
        }

        try {
            // Create the index.xml file with some properties in it.
            File indexXmlFile = new File(jukebox.getJukeboxTempLocation(), "index.xml");
            indexXmlFile.getParentFile().mkdirs();
            XMLWriter writer = new XMLWriter(indexXmlFile);

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
            FileTools.addJukeboxFile(indexXmlFile.getName());
            FileTools.addJukeboxFile(htmlFile.getName());

            Transformer transformer = getTransformer(new File(skinHome, "jukebox-index.xsl"), jukebox.getJukeboxTempLocation());

            Source xmlSource = new StreamSource(indexXmlFile);
            Result xmlResult = new StreamResult(htmlFile);

            doTransform(transformer, xmlSource, xmlResult, "Jukebox index");
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed generating jukebox index.");
            logger.error(SystemTools.getStackTrace(error));
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
                    logger.info(LOG_MESSAGE + "No categories were found, so you should specify mjb.homePage in the config file.");
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
            writer.writeAttribute("content", "0; url=" + jukebox.getDetailsDirName() + '/' + homePage + EXT_HTML);
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndElement();
            writer.close();
            fos.close();
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed generating HTML library index.");
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    public void generateMoviesCategoryHTML(Jukebox jukebox, Library library, String filename, String template, boolean isDirty) {
        try {
            // Issue 1886: Html indexes recreated every time
            String destFolder = jukebox.getJukeboxRootLocationDetails();
            File oldFile = FileTools.fileCache.getFile(destFolder + File.separator + filename + EXT_HTML);
            if (oldFile.exists() && !isDirty) {
                return;
            }

            logger.info("  " + filename + "...");

            Source xmlSource;
            File detailsFolder = jukebox.getJukeboxTempLocationDetailsFile();
            File xmlFile = new File(detailsFolder, filename + EXT_XML);
            File oldXmlFile = FileTools.fileCache.getFile(jukebox.getJukeboxRootLocationDetails() + File.separator + filename + EXT_XML);
            File htmlFile = new File(detailsFolder, filename + EXT_HTML);

            htmlFile.getParentFile().mkdirs();

            if (xmlFile.exists()) {
                FileTools.addJukeboxFile(xmlFile.getName());
                xmlSource = new StreamSource(xmlFile);
            } else {
                xmlSource = new StreamSource(oldXmlFile);
            }

            FileTools.addJukeboxFile(htmlFile.getName());
            Result xmlResult = new StreamResult(htmlFile);

            Transformer transformer = getTransformer(new File(skinHome, template), jukebox.getJukeboxTempLocation());
            doTransform(transformer, xmlSource, xmlResult, "Playlist generation");

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed generating HTML library category index.");
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    public void generateMoviesIndexHTML(final Jukebox jukebox, final Library library, ThreadExecutor<Void> tasks) throws Throwable {
        tasks.restart();
        for (final IndexInfo idx : library.getGeneratedIndexes()) {
            if (idx.canSkip) { // this is evaluated during XML indexing
                logger.debug(LOG_MESSAGE + "Category " + idx.categoryName + " " + idx.key + " no change detected, skipping HTML generation.");

                // Add the index files to the cache so they aren't deleted
                for (int page = 1; page <= idx.pages; page++) {
                    FileTools.addJukeboxFile(idx.baseName + page + EXT_XML);
                    FileTools.addJukeboxFile(idx.baseName + page + EXT_HTML);
                }
            } else {
                logger.debug(LOG_MESSAGE + "Category " + idx.categoryName + " " + idx.key + " generate HTML.");
                tasks.submit(new Callable<Void>() {
                    @Override
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

    private void writeSingleIndexPage(Jukebox jukebox, IndexInfo idx, int page) {
        try {
            File detailsDir = jukebox.getJukeboxTempLocationDetailsFile();
            detailsDir.mkdirs();

            String filename = idx.baseName + page;

            File xmlFile = new File(detailsDir, filename + EXT_XML);

            FileTools.addJukeboxFile(xmlFile.getName());
            String indexList = PropertiesUtil.getProperty("mjb.view.indexList", "index.xsl");

            for (final String indexStr : indexList.split(",")) {
                String suffix = "";
                if (!indexStr.equals("index.xsl")) {
                    suffix = indexStr.replace("index", "").replace(EXT_XSL, "");
                }

                File htmlFile = new File(detailsDir, filename + suffix + EXT_HTML);
                FileTools.addJukeboxFile(htmlFile.getName());

                File transformCatKey = new File(skinHome, FileTools.makeSafeFilename(idx.categoryName + "_" + idx.key) + EXT_XSL);
                File transformCategory = new File(skinHome, FileTools.makeSafeFilename(idx.categoryName) + EXT_XSL);
                File transformBase = new File(skinHome, indexStr);

                Transformer transformer;

                if (transformCatKey.exists()) {
                    logger.debug(LOG_MESSAGE + "Using CategoryKey transformation " + transformCatKey.getName() + " for " + xmlFile.getName());
                    transformer = getTransformer(transformCatKey, jukebox.getJukeboxTempLocationDetails());
                } else if (transformCategory.exists()) {
                    logger.debug(LOG_MESSAGE + "Using Category transformation " + transformCategory.getName() + " for " + xmlFile.getName());
                    transformer = getTransformer(transformCategory, jukebox.getJukeboxTempLocationDetails());
                } else {
                    transformer = getTransformer(transformBase, jukebox.getJukeboxTempLocationDetails());
                }

                // Transformer transformer = getTransformer(new File(skinHome, "index.xsl"), rootPath);
                Source xmlSource = new StreamSource(xmlFile);
                Result xmlResult = new StreamResult(htmlFile);

                doTransform(transformer, xmlSource, xmlResult, "Category page");
            }
        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed generating HTML library index for Category: " + idx.categoryName + ", Key: " + idx.key + ", Page: " + page);
            logger.error(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Creates and caches Transformer, one for every thread/xsl file.
     */
    public static Transformer getTransformer(File xslFile, String styleSheetTargetRootPath) {
        /*
         * Removed caching of transformer, as saxon keeps all parsed documents in memory, causing memory leaks.
         * Creating a new transformer every time doesn't consume too much time and has no impact on performance.
         * It lets YAMJ save lot of memory.
         * @author Vincent
         */
        Source xslSource = new StreamSource(xslFile);

        // Sometimes the StreamSource doesn't return an object and we get a null pointer exception, so check it and try loading it again
        for (int looper = 1; looper < 5; looper++) {
            xslSource = new StreamSource(xslFile);
            if (xslSource != null) {
                // looks ok, so quit the loop
                break;
            }
        }

        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer(xslSource);
            transformer.setParameter("homePage", indexHtmFile);
            transformer.setParameter("rootPath", new File(styleSheetTargetRootPath).getAbsolutePath().replace('\\', '/'));
            for (Entry<Object, Object> e : PropertiesUtil.getEntrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    transformer.setParameter(e.getKey().toString(), e.getValue().toString());
                }
            }
        } catch (TransformerConfigurationException ex) {
            logger.warn(SystemTools.getStackTrace(ex));
        }
        return transformer;
    }

    /**
     * Try to safely perform the transformation. Will retry up to maxRetryCount times before throwing the error
     *
     * @param transformer
     * @param xmlSource
     * @param xmlResult
     * @param message Message to print if there is an error
     * @throws Exception
     */
    private void doTransform(Transformer transformer, Source xmlSource, Result xmlResult, String message) throws Exception {
        int retryCount = 0;

        do {
            try {
                transformer.transform(xmlSource, xmlResult);
                return;  // If the transform didn't throw an error, return
            } catch (TransformerException ex) {
                int retryTimes = maxRetryCount - ++retryCount;

                if (retryTimes == 0) {
                    // We've exceeded the maximum number of retries, so throw the exception and quit
                    throw new Exception(ex);
                } else {
                    logger.debug(LOG_MESSAGE + "Failed generating HTML, will retry "
                            + retryTimes + " more time" + (retryTimes == 1 ? ". " : "s. ") + message);
                    Thread.sleep(500);  // Sleep for 1/2 second to hopefully let the issue go away
                }
            }   // Catch
        } while (retryCount <= maxRetryCount);
    }
}
