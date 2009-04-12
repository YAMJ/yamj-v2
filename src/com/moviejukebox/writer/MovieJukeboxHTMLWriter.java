package com.moviejukebox.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Generate HTML pages from XML movies and indexes
 * 
 * @author Julien
 */
public class MovieJukeboxHTMLWriter {

    private static Logger logger = Logger.getLogger("moviejukebox");
    private boolean forceHTMLOverwrite;
    private int nbMoviesPerPage;
    private String skinHome;

    public MovieJukeboxHTMLWriter() {
        forceHTMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceHTMLOverwrite", "false"));
        nbMoviesPerPage = Integer.parseInt(PropertiesUtil.getProperty("mjb.nbThumbnailsPerPage", "10"));
        skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");

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
            File oldXmlFile = new File(rootPath + File.separator + baseName + ".xml");
            File finalHtmlFile = new File(rootPath + File.separator + baseName + ".html");
            File tempHtmlFile = new File(tempFilename + ".html");
            Source xmlSource;

            if (!finalHtmlFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
                tempHtmlFile.getParentFile().mkdirs();

                TransformerFactory tranformerFactory = TransformerFactory.newInstance();

                Source xslSource = new StreamSource(new File(skinHome + File.separator + "detail.xsl"));
                Transformer transformer = tranformerFactory.newTransformer(xslSource);

                // Issue 216: If the HTML is deleted the generation fails because it looks in the temp directory and not
                // the original source directory
                if (tempXmlFile.exists()) {
                    // Use the temp file
                    xmlSource = new StreamSource(new FileInputStream(tempXmlFile));
                } else {
                    // Use the file in the original directory
                    xmlSource = new StreamSource(new FileInputStream(oldXmlFile));
                }
                FileOutputStream outStream = new FileOutputStream(tempHtmlFile);
                Result xmlResult = new StreamResult(outStream);

                transformer.transform(xmlSource, xmlResult);
                outStream.flush();
                outStream.close();
            }
        } catch (Exception e) {
            System.err.println("Failed generating HTML for movie " + movie);
            e.printStackTrace();
        }
    }

    public void generatePlaylist(String rootPath, String tempRootPath, Movie movie) {
        try {
            String myiHomeIP = PropertiesUtil.getProperty("mjb.myiHome.IP", "");
            if (movie.getFiles().size() > 1) {
                String baseName = FileTools.makeSafeFilename(movie.getBaseName());
                String tempFilename = tempRootPath + File.separator + baseName;
                File finalPlaylistFile = new File(rootPath + File.separator + baseName + ".playlist.jsp");
                File tempPlaylistFile = new File(tempFilename + ".playlist.jsp");

                if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
                    tempPlaylistFile.getParentFile().mkdirs();

                    PrintWriter writer = new PrintWriter(tempPlaylistFile, "UTF-8");

                    // Issue 237 - Add in the IP address of the MyiHome server so the playlist will work.
                    // Issue 237 - It is perfectly valid for "mjb.myiHome.IP" to be blank, in fact this is the the
                    // normal method for standalone YAMJ
                    for (MovieFile part : movie.getFiles()) {
                        // write one line each in the format "name|0|0|IP/path" replacing an | that may exist in the
                        // title
                        writer.println(movie.getTitle().replace('|', ' ') + " " + part.getFirstPart() + "|0|0|" + myiHomeIP + part.getFilename() + "|");
                    }
                    writer.flush();
                    writer.close();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed generating playlist for movie " + movie);
            e.printStackTrace();
        }
    }

    public void generateMoviesCategoryHTML(String rootPath, String detailsDirName, Library library) {
        try {
            File detailsFolder = new File(rootPath, detailsDirName);
            String filename = "Categories";
            File xmlFile = new File(detailsFolder, filename + ".xml");
            File htmlFile = new File(detailsFolder, filename + ".html");

            htmlFile.getParentFile().mkdirs();

            TransformerFactory tranformerFactory = TransformerFactory.newInstance();

            Source xslSource = new StreamSource(new File(skinHome, "categories.xsl"));
            Transformer transformer = tranformerFactory.newTransformer(xslSource);

            Source xmlSource = new StreamSource(new FileInputStream(xmlFile));
            FileOutputStream outStream = new FileOutputStream(htmlFile);
            Result xmlResult = new StreamResult(outStream);

            transformer.transform(xmlSource, xmlResult);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            System.err.println("Failed generating HTML library category index.");
            e.printStackTrace();
        }
    }

    public void generateMoviesIndexHTML(String rootPath, String detailsDirName, Library library) {
        for (Map.Entry<String, Library.Index> category : library.getIndexes().entrySet()) {
            String categoryName = category.getKey();
            Map<String, List<Movie>> index = category.getValue();

            for (Map.Entry<String, List<Movie>> indexEntry : index.entrySet()) {
                String key = indexEntry.getKey();
                List<Movie> movies = indexEntry.getValue();
                if (!movies.isEmpty()) {
                    int nbPages = 1 + (movies.size() - 1) / nbMoviesPerPage;
                    for (int page = 1; page <= nbPages; page++) {
                        writeSingleIndexPage(rootPath, detailsDirName, categoryName, key, page);
                    }
                }
            }
        }
        try {
            File htmlFile = new File(rootPath, PropertiesUtil.getProperty("mjb.indexFile", "index.htm"));
            htmlFile.getParentFile().mkdirs();

            FileOutputStream fos = new FileOutputStream(htmlFile);
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
            writer.writeAttribute("HTTP-EQUIV", "REFRESH");
            writer.writeAttribute("content", "0; url=" + detailsDirName + '/' + homePage + ".html");
            writer.writeEndElement();

            writer.writeEndElement();
            writer.writeEndElement();
            writer.close();
            fos.close();
        } catch (Exception e) {
            System.err.println("Failed generating HTML library index.");
            e.printStackTrace();
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

            TransformerFactory tranformerFactory = TransformerFactory.newInstance();

            Source xslSource = new StreamSource(new File(skinHome, "index.xsl"));
            Transformer transformer = tranformerFactory.newTransformer(xslSource);

            FileOutputStream outStream = new FileOutputStream(htmlFile);
            Source xmlSource = new StreamSource(new FileInputStream(xmlFile));
            Result xmlResult = new StreamResult(outStream);

            transformer.transform(xmlSource, xmlResult);
            outStream.flush();
            outStream.close();
        } catch (Exception e) {
            System.err.println("Failed generating HTML library index.");
            e.printStackTrace();
        }
    }
}
