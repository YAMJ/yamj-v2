/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

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

import com.moviejukebox.model.Index;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;

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
    private static TransformerFactory transformerFactory;
    private static final Map<String, Transformer> transformerCache = new HashMap<String, Transformer>();
    private static String str_categoriesIndexList = PropertiesUtil.getProperty("mjb.categories.indexList", "Other,Genres,Title,Rating,Year,Library,Set");
    private static List<String> categoriesIndexList;
    private static int categoriesMinCount = Integer.parseInt(PropertiesUtil.getProperty("mjb.categories.minCount", "3"));
    private static String playlistIgnoreExtensions = PropertiesUtil.getProperty("mjb.playlist.IgnoreExtensions", "iso,img");
    private static File playlistFile;
    private static String indexFile = "../" + PropertiesUtil.getProperty("mjb.indexFile", "index.htm");
    private static String myiHomeIP = PropertiesUtil.getProperty("mjb.myiHome.IP", "");
    private static boolean generateMultiPartPlaylist = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.playlist.generateMultiPart", "true"));

    static {
        categoriesIndexList = Arrays.asList(str_categoriesIndexList.split(","));
    }

    public MovieJukeboxHTMLWriter() {
        forceHTMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceHTMLOverwrite", "false"));
        nbMoviesPerPage = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbThumbnailsPerPage", "10"));
        nbTvShowsPerPage = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbTvThumbnailsPerPage", "0")); // If 0 then use the Movies setting
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

    public void generateMovieDetailsHTML(String rootPath, String tempRootPath, Movie movie) {
        try {
            String baseName = FileTools.makeSafeFilename(movie.getBaseName());
            String tempFilename = tempRootPath + File.separator + baseName;
            File tempXmlFile = new File(tempFilename + ".xml");
            File oldXmlFile = FileTools.fileCache.getFile(rootPath + File.separator + baseName + ".xml");
            File finalHtmlFile = FileTools.fileCache.getFile(rootPath + File.separator + baseName + ".html");
            File tempHtmlFile = new File(tempFilename + ".html");
            Source xmlSource;

            FileTools.addJukeboxFile(baseName + ".xml");
            FileTools.addJukeboxFile(baseName + ".html");

            if (!finalHtmlFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
                Transformer transformer = getTransformer(new File(skinHome,  "detail.xsl"), rootPath);

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

                transformer.transform(xmlSource, xmlResult);
            }
        } catch (Exception error) {
            logger.severe("Failed generating HTML for movie " + movie);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    /**
     * Generates a playlist per part of the video. Used primarily with TV Series
     * @param   rootPath
     * @param   tempRootPath
     * @param   movie
     * @return  List of generated file names
     */
    public Collection<String> generatePlaylist(String rootPath, String tempRootPath, Movie movie) {
        Collection<String> fileNames = new ArrayList<String>();

        if (playlistFile == null || playlistFile.equals("")) {
            return fileNames;
        }
        
        MovieFile[] movieFileArray = movie.getFiles().toArray(new MovieFile[movie.getFiles().size()]);

        try {
            String baseName = FileTools.makeSafeFilename(movie.getBaseName());
            String tempFilename = tempRootPath + File.separator + baseName;
            File tempXmlFile = new File(tempFilename + ".xml");
            File oldXmlFile = new File(rootPath + File.separator + baseName + ".xml");
            final String filenameSuffix = ".playlist.jsp";
            File finalPlaylistFile = new File(rootPath + File.separator + baseName + filenameSuffix);
            File tempPlaylistFile = new File(tempFilename + filenameSuffix);
            Source xmlSource;
            
            fileNames.add(baseName + filenameSuffix);

            // Issue 884: Remove ISO and IMG files from playlists
            int partCount = 0;
            for (int i = 0; i < movieFileArray.length; i++) {
                MovieFile moviePart = movieFileArray[i];
                String partExt = moviePart.getFilename().substring(moviePart.getFilename().lastIndexOf(".") + 1);
                if (playlistIgnoreExtensions.indexOf(partExt) > -1) partCount++;
            }
            if (partCount > 0) {
                // Note this will skip playlist generation for any movie that has an "mjb.playlist.ignoreextensions" entry.
                logger.finest("Playlist for " + movie.getTitle() + " skipped - All parts are in mjb.playlist.IgnoreExtensions");
                return fileNames;
            } // Issue 884
            
            if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
                tempPlaylistFile.getParentFile().mkdirs();

                Transformer transformer = getTransformer(playlistFile, rootPath);

                if (tempXmlFile.exists()) {
                    // Use the temp file
                    xmlSource = new StreamSource(tempXmlFile);
                } else {
                    // Use the file in the original directory
                    xmlSource = new StreamSource(oldXmlFile);
                }
                Result xmlResult = new StreamResult(tempPlaylistFile);

                transformer.transform(xmlSource, xmlResult);

                fileNames.add(baseName + filenameSuffix);
            }
        } catch (Exception error) {
            logger.severe("Failed generating playlist for movie " + movie);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        
        // if the multi part playlists are not required
        if (generateMultiPartPlaylist) {
            try {
                if (movie.getFiles().size() > 1) {
                    for (int i = 0; i < movieFileArray.length; i++) {
                        fileNames.add(generateSimplePlaylist(rootPath, tempRootPath, movie, movieFileArray, i));
                    }
                }
            } catch (Exception error) {
                logger.severe("Failed generating playlist for movie " + movie);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }
        
        return fileNames;
    }

    /**
     * Generate playlist with old simple method. The playlist is to be used for playing episodes
     * starting from each episode separately.
     * @param rootPath
     * @param tempRootPath
     * @param movie
     * @param movieFiles
     * @param offset
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @return generated file name
     */
    private String generateSimplePlaylist(String rootPath, String tempRootPath, Movie movie, MovieFile[] movieFiles, int offset) 
            throws FileNotFoundException, UnsupportedEncodingException {
        String fileSuffix = ".playlist"+ movieFiles[offset % movieFiles.length].getFirstPart() + ".jsp";
        String baseName = FileTools.makeSafeFilename(movie.getBaseName());
        String tempFilename = tempRootPath + File.separator + baseName;
        File finalPlaylistFile = new File(rootPath + File.separator + baseName + fileSuffix);
        File tempPlaylistFile = new File(tempFilename + fileSuffix);

        if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
            tempPlaylistFile.getParentFile().mkdirs();

            PrintWriter writer = new PrintWriter(tempPlaylistFile, "UTF-8");

            // Issue 237 - Add in the IP address of the MyiHome server so the playlist will work.
            // Issue 237 - It is perfectly valid for "mjb.myiHome.IP" to be blank, in fact this is
            //             the normal method for stand alone YAMJ
            for (int i = 0; i < movieFiles.length; i++) {
                MovieFile part = movieFiles[ (i + offset) % movieFiles.length];
                // write one line each in the format "name|0|0|IP/path" replacing an | that may exist in the title
                writer.println(movie.getTitle().replace('|', ' ') + " " + part.getFirstPart() + "|0|0|" + myiHomeIP + part.getFilename() + "|");
            }
            writer.flush();
            writer.close();
        }
        return baseName + fileSuffix;
    }

    public void generateMoviesCategoryHTML(String rootPath, String detailsDirName, Library library) {
        try {
            File detailsFolder = new File(rootPath, detailsDirName);
            String filename = "Categories";
            File xmlFile = new File(detailsFolder, filename + ".xml");
            File htmlFile = new File(detailsFolder, filename + ".html");

            htmlFile.getParentFile().mkdirs();

            FileTools.addJukeboxFile(xmlFile.getName());
            FileTools.addJukeboxFile(htmlFile.getName());

            Transformer transformer = getTransformer(new File(skinHome, "categories.xsl"), rootPath);

            Source xmlSource = new StreamSource(xmlFile);
            Result xmlResult = new StreamResult(htmlFile);

            transformer.transform(xmlSource, xmlResult);
        } catch (Exception error) {
            logger.severe("Failed generating HTML library category index.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    public void generateMoviesIndexHTML(final String rootPath, final String detailsDirName, final Library library, int threadcount) throws Throwable {
        ThreadExecutor<Void> tasks = new ThreadExecutor<Void>(threadcount);
        for (Map.Entry<String, Index> category : library.getIndexes().entrySet()) {
            final String categoryName = category.getKey();

            if (!categoriesIndexList.contains(categoryName)) continue;

            Map<String, List<Movie>> index = category.getValue();

            for (final Map.Entry<String, List<Movie>> indexEntry : index.entrySet()) {
                final String key = indexEntry.getKey();
                if(library.isIndexSkipped(indexEntry)){
                    logger.finer("Category " + categoryName + "/" + key + ", skipping HTML generation.");
                    continue;
                }

                tasks.submit(new Callable<Void>() {
                    public Void call() {
                        List<Movie> movies  = indexEntry.getValue();

                        int nbVideosPerPage;
                        if (key.equalsIgnoreCase("TV Shows") || (categoryName.equalsIgnoreCase("Set") && movies.get(0).isTVShow())) {
                            nbVideosPerPage = nbTvShowsPerPage;
                        } else {
                            nbVideosPerPage = nbMoviesPerPage;
                        }

                        //FIXME This is horrible! Issue 735 will get rid of it.
                        int countNbMovies= library.getMovieCountForIndex(categoryName, key);
                        if (countNbMovies >= categoriesMinCount || Arrays.asList("Other,Genres,Title,Year,Library,Set".split(",")).contains(categoryName)) {
                            int nbPages = 1 + (movies.size() - 1) / nbVideosPerPage;
                            for (int page = 1; page <= nbPages; page++) {
                                writeSingleIndexPage(rootPath, detailsDirName, categoryName, key, page);
                            }
                        }
                        return null;
                    };
                });
            };
        }

        tasks.waitFor();

        try {
            File htmlFile = new File(rootPath, PropertiesUtil.getProperty("mjb.indexFile", "index.htm"));
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
                    logger.fine("No categories were found, so you should specify mjb.homePage in the config file.");
                }
            }

            writer.writeStartDocument();
            writer.writeStartElement("html");
            writer.writeStartElement("head");

            writer.writeStartElement("meta");
            writer.writeAttribute("name", "Author");
            writer.writeAttribute("content", "MovieJukebox");
            writer.writeEndElement();

            writer.writeStartElement("meta");
            writer.writeAttribute("HTTP-EQUIV", "Content-Type");
            writer.writeAttribute("content", "text/html; charset=UTF-8");
            writer.writeEndElement();

            writer.writeStartElement("meta");
            writer.writeAttribute("HTTP-EQUIV", "REFRESH");
            writer.writeAttribute("content", "0; url=" + detailsDirName + '/' + homePage + ".html");
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndElement();
            writer.close();
            fos.close();
        } catch (Exception error) {
            logger.severe("Failed generating HTML library index.");
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    private void writeSingleIndexPage(String rootPath, String detailsDirName, String categoryName, String key, int page)
                    throws TransformerFactoryConfigurationError {
        try {
            File detailsDir = new File(rootPath, detailsDirName);
            detailsDir.mkdirs();

            String filename = FileTools.makeSafeFilename(FileTools.createPrefix(categoryName, key)) + page;

            File xmlFile = new File(detailsDir, filename + ".xml");
            File htmlFile = new File(detailsDir, filename + ".html");
            
            FileTools.addJukeboxFile(xmlFile.getName());
            FileTools.addJukeboxFile(htmlFile.getName());
            
            File transformCatKey = new File(skinHome, FileTools.makeSafeFilename(categoryName + "_" + key) + ".xsl");
            File transformCategory = new File(skinHome, FileTools.makeSafeFilename(categoryName) + ".xsl");
            File transformBase = new File(skinHome, "index.xsl");

            Transformer transformer;
            
            if (transformCatKey.exists()) {
                logger.finest("HTMLWriter: Using CategoryKey transformation " + transformCatKey.getName() + " for " + xmlFile.getName());
                transformer = getTransformer(transformCatKey, rootPath);
            } else if (transformCategory.exists()) {
                logger.finest("HTMLWriter: Using Category transformation " + transformCategory.getName() + " for " + xmlFile.getName());
                transformer = getTransformer(transformCategory, rootPath);
            } else {
                transformer = getTransformer(transformBase, rootPath);
            }

            //Transformer transformer = getTransformer(new File(skinHome, "index.xsl"), rootPath);
            Source xmlSource = new StreamSource(xmlFile);
            Result xmlResult = new StreamResult(htmlFile);

            transformer.transform(xmlSource, xmlResult);
        } catch (Exception error) {
            logger.severe("Failed generating HTML library index for Category: " + categoryName + ", Key: " + key + ", Page: " + page);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    /**
     * Creates and caches Transformer, one for every xsl file.
     */
    public static synchronized Transformer getTransformer(File xslFile, String styleSheetTargetRootPath) throws TransformerConfigurationException {
        // Gabriel: transformers are NOT thread safe; use thread name to make get the cache thread specific
        // the method itself must be synchronized because transformerCache map is modified inside
        String lookupID = Thread.currentThread().getId() + ":" + xslFile.getAbsolutePath();
        if (! transformerCache.containsKey(lookupID)) {
            if (transformerFactory == null) {
                transformerFactory = TransformerFactory.newInstance();
            }
            Source xslSource = new StreamSource(xslFile);
            Transformer transformer = transformerFactory.newTransformer(xslSource);
            transformer.setParameter("homePage", indexFile);
            transformer.setParameter("rootPath", new File(styleSheetTargetRootPath).getAbsolutePath().replace('\\', '/'));
            for (Entry<Object, Object> e : PropertiesUtil.getEntrySet()) {
                if (e.getKey() != null && e.getValue() != null)
                    transformer.setParameter(e.getKey().toString(), e.getValue().toString());
            }
            transformerCache.put(lookupID, transformer);
        }
        return transformerCache.get(lookupID);
    }
}
